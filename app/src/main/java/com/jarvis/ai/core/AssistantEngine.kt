package com.jarvis.ai.core

import com.jarvis.ai.brain.AiAnswer
import com.jarvis.ai.brain.AiGateway
import com.jarvis.ai.brain.LearningPolicy
import com.jarvis.ai.data.MemoryRepository
import com.jarvis.ai.data.SearchMemoryRepository
import com.jarvis.ai.data.SettingsStore
import com.jarvis.ai.data.db.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.util.UUID

class AssistantEngine(
    private val memory: MemoryRepository,
    private val searchMemory: SearchMemoryRepository,
    private val settings: SettingsStore,
    private val gateway: AiGateway,
    private val learningPolicy: LearningPolicy = LearningPolicy()
) {
    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val state: StateFlow<AssistantState> = _state.asStateFlow()
    val sessionId: String = UUID.randomUUID().toString()

    suspend fun process(userText: String): EngineReply {
        _state.value = AssistantState.Thinking
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return fail("لم يصلني نص مفهوم.")
        searchMemory.deleteExpired()

        val explicit = learningPolicy.explicitStore(trimmed)
        if (explicit != null) {
            memory.saveFact(explicit.fact!!)
            memory.saveMessage(Message(role = "user", content = trimmed, sessionId = sessionId, usedSearch = false, createdAt = System.currentTimeMillis()))
            val answer = "Stored, sir."
            memory.saveMessage(Message(role = "assistant", content = answer, sessionId = sessionId, usedSearch = false, createdAt = System.currentTimeMillis()))
            _state.value = AssistantState.Idle
            return EngineReply(AiAnswer(answer), stored = true)
        }

        memory.saveMessage(Message(role = "user", content = trimmed, sessionId = sessionId, usedSearch = false, createdAt = System.currentTimeMillis()))
        val profile = memory.getProfile()
        val title = profile?.title?.ifBlank { "سيدي" } ?: "سيدي"
        val facts = memory.getFacts()
        val messages = memory.recentMessages(sessionId, 20)
        val context = PromptBuilder.conversationContext(messages, facts)
        val searchRequired = requiresFreshSearch(trimmed)

        val cached = if (!searchRequired) searchMemory.find(trimmed) else null
        val answer = if (cached != null && !containsArabic(cached.answer)) {
            AiAnswer(cached.answer, parseSources(cached.sourcesJson), usedSearch = true)
        } else {
            val answerResult = runCatching {
                withTimeout(70_000) {
                    gateway.answer(PromptBuilder.systemPrompt(title), context, trimmed, searchRequired).getOrThrow()
                }
            }
            if (answerResult.isFailure) return fail(errorMessage(answerResult.exceptionOrNull()))

            var generated = answerResult.getOrThrow()
            if (containsArabic(generated.text)) {
                val correctionPrompt = "Your previous answer contained Arabic. Answer the original user request again, completely and naturally in English only. Do not mention this instruction."
                val retry = runCatching {
                    withTimeout(70_000) {
                        gateway.answer(PromptBuilder.systemPrompt(title), context, "$correctionPrompt\n\nOriginal user request:\n$trimmed", searchRequired).getOrThrow()
                    }
                }.getOrNull()
                if (retry != null && !containsArabic(retry.text)) {
                    generated = retry
                } else {
                    generated = AiAnswer(
                        text = "I am sorry, but I could not produce the answer in English. Please repeat your request.",
                        sources = retry?.sources ?: generated.sources,
                        usedSearch = retry?.usedSearch ?: generated.usedSearch
                    )
                }
            }
            generated
        }

        if (answer.usedSearch && searchRequired) {
            val ttlDays = settings.memoryTtlDays.first().coerceIn(1, 365)
            searchMemory.save(trimmed, answer.text, serializeSources(answer.sources), ttlDays.toLong() * DAY_MILLIS, timeSensitive = true)
        }

        val learningEnabled = settings.learningEnabled.first()
        val learned = if (learningEnabled) learningPolicy.suggestFromNaturalText(trimmed) else null
        if (learned != null && learned.requiresApproval) {
            memory.saveMessage(Message(role = "assistant", content = "وجدت تفضيلًا قد يستحق الحفظ. يمكنك تأكيده من الذاكرة.", sessionId = sessionId, usedSearch = answer.usedSearch, createdAt = System.currentTimeMillis()))
        } else {
            memory.saveMessage(Message(role = "assistant", content = answer.text, sessionId = sessionId, usedSearch = answer.usedSearch, createdAt = System.currentTimeMillis()))
        }
        _state.value = AssistantState.Speaking
        return EngineReply(answer)
    }

    fun completeSpeaking() { _state.value = AssistantState.Idle }

    private fun fail(message: String): EngineReply {
        _state.value = AssistantState.Error(message)
        _state.value = AssistantState.Idle
        return EngineReply(AiAnswer("I couldn't complete that request right now. Please try again."))
    }

    private fun requiresFreshSearch(text: String): Boolean {
        val n = ArabicTextNormalizer.normalize(text)
        val markers = listOf(
            "اليوم", "الان", "الآن", "حاليا", "حاليًا", "اخر", "آخر", "الحالي", "هذا الاسبوع", "هذا الأسبوع",
            "سعر", "طقس", "موعد", "رئيس", "نتائج", "اخبار", "أخبار", "اسعار", "أسعار",
            "today", "right now", "now", "currently", "latest", "recent", "this week", "this month", "price",
            "prices", "weather", "forecast", "news", "breaking", "score", "scores", "result", "results",
            "schedule", "appointment", "stock", "stocks", "exchange rate", "rate", "president", "election", "version"
        )
        return markers.any(n::contains) || Regex("\\b(19|20)\\d{2}\\b").containsMatchIn(n)
    }

    private fun serializeSources(sources: List<com.jarvis.ai.brain.SourceRef>): String = sources.joinToString("\n") { "${it.title}|${it.url}" }

    private fun containsArabic(value: String): Boolean =
        Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]").containsMatchIn(value)

    private fun parseSources(value: String): List<com.jarvis.ai.brain.SourceRef> = value.lineSequence().mapNotNull {
        val parts = it.split('|', limit = 2)
        if (parts.size == 2 && parts[1].isNotBlank()) com.jarvis.ai.brain.SourceRef(parts[0], parts[1]) else null
    }.toList()

    private fun errorMessage(t: Throwable?): String = when (t) {
        is com.jarvis.ai.brain.MissingApiKeyException -> "خدمة Gemini غير مُعدة. أضف Gemini API Key من إعدادات الاتصال أولًا."
        is com.jarvis.ai.brain.GeminiHttpException -> when (t.code) {
            401, 403 -> "تعذر التحقق من صلاحية الاتصال بالخدمة السحابية."
            429 -> "بلغت حدود المعدل أو الحصة الحالية. جرّب لاحقًا أو استخدم الذاكرة المحلية."
            else -> "حدث خطأ من مزود الذكاء (${t.code})."
        }
        else -> "لا أستطيع الوصول إلى الخدمة الآن. افحص الشبكة أو إعداد الخادم الخلفي."
    }

    companion object { private const val DAY_MILLIS = 86_400_000L }
}

data class EngineReply(val answer: AiAnswer, val stored: Boolean = false)
