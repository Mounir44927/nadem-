package com.jarvis.ai

import android.app.Application
import androidx.room.Room
import com.jarvis.ai.brain.ConfiguredAiGateway
import com.jarvis.ai.data.GeminiApiKeyStore
import com.jarvis.ai.data.MemoryRepository
import com.jarvis.ai.data.SearchMemoryRepository
import com.jarvis.ai.data.SettingsStore
import com.jarvis.ai.data.db.AppDatabase

class JarvisApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var memory: MemoryRepository
        private set
    lateinit var searchMemory: SearchMemoryRepository
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var geminiKeyStore: GeminiApiKeyStore
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "jarvis.db").build()
        memory = MemoryRepository(db)
        searchMemory = SearchMemoryRepository(db.dao())
        settings = SettingsStore(this)
        geminiKeyStore = GeminiApiKeyStore(this)
    }

    fun createGateway() = ConfiguredAiGateway(geminiKeyStore)
}
