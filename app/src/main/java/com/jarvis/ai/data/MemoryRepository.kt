package com.jarvis.ai.data

import com.jarvis.ai.data.db.AppDatabase
import com.jarvis.ai.data.db.Fact
import com.jarvis.ai.data.db.MasterProfile
import com.jarvis.ai.data.db.Message
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    suspend fun getProfile(): MasterProfile? = dao.getProfile()
    suspend fun saveProfile(profile: MasterProfile) = dao.upsertProfile(profile)
    fun observeFacts(): Flow<List<Fact>> = dao.observeFacts()
    suspend fun getFacts(): List<Fact> = dao.getFacts()
    suspend fun saveFact(fact: Fact) = dao.insertFact(fact)
    suspend fun deleteFact(id: Long) = dao.deleteFact(id)
    suspend fun deleteAllMemory() { dao.deleteAllFacts(); dao.deleteAllMessages() }
    suspend fun saveMessage(message: Message) = dao.insertMessage(message)
    suspend fun recentMessages(sessionId: String, limit: Int = 20): List<Message> = dao.getRecentMessages(sessionId, limit).reversed()
    fun observeMessages(sessionId: String): Flow<List<Message>> = dao.observeMessages(sessionId)
}
