package com.jarvis.ai.data

import com.jarvis.ai.core.ArabicTextNormalizer
import com.jarvis.ai.data.db.AppDao
import com.jarvis.ai.data.db.SearchMemory

class SearchMemoryRepository(private val dao: AppDao) {
    suspend fun find(query: String): SearchMemory? {
        val normalized = ArabicTextNormalizer.normalize(query)
        if (normalized.isBlank()) return null
        return dao.findValidSearch(normalized, System.currentTimeMillis())
    }

    suspend fun save(query: String, answer: String, sourcesJson: String, ttlMillis: Long, timeSensitive: Boolean) {
        val normalized = ArabicTextNormalizer.normalize(query)
        if (normalized.isBlank()) return
        val now = System.currentTimeMillis()
        dao.insertSearchMemory(
            SearchMemory(
                normalizedQuery = normalized,
                query = query,
                answer = answer,
                sourcesJson = sourcesJson,
                createdAt = now,
                updatedAt = now,
                expiresAt = now + ttlMillis,
                timeSensitive = timeSensitive
            )
        )
    }

    suspend fun deleteExpired(now: Long = System.currentTimeMillis()) = dao.deleteExpiredSearch(now)
}
