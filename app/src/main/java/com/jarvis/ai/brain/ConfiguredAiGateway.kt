package com.jarvis.ai.brain

import com.jarvis.ai.BuildConfig
import com.jarvis.ai.data.GeminiApiKeyStore
import kotlinx.coroutines.flow.first

class ConfiguredAiGateway(
    private val keyStore: GeminiApiKeyStore,
    private val direct: AiGateway = DirectGeminiGateway(keyStore),
    private val backend: AiGateway = BackendGeminiGateway()
) : AiGateway {
    override suspend fun answer(systemPrompt: String, context: String, userPrompt: String, search: Boolean): Result<AiAnswer> {
        if (keyStore.isConfigured.first()) {
            return direct.answer(systemPrompt, context, userPrompt, search)
        }
        if (BackendEndpointValidator.isValidBaseUrl(BuildConfig.BACKEND_BASE_URL)) {
            return backend.answer(systemPrompt, context, userPrompt, search)
        }
        return Result.failure(MissingApiKeyException())
    }
}
