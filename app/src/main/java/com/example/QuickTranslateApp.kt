package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.BillingRepository
import com.example.data.repository.TranslationRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.tts.TtsManager

class QuickTranslateApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var userPreferences: UserPreferencesRepository
        private set
    lateinit var billingRepository: BillingRepository
        private set
    lateinit var translationRepository: TranslationRepository
        private set
    lateinit var ttsManager: TtsManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        userPreferences = UserPreferencesRepository(this)
        billingRepository = BillingRepository(this)
        translationRepository = TranslationRepository(
            translationDao = database.translationDao(),
            userPreferences = userPreferences,
            billingRepository = billingRepository
        )
        ttsManager = TtsManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
    }
}
