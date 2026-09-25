package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.BillingRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.model.Language
import com.example.domain.translation.BusinessTranslator
import com.example.domain.translation.LanguageDetector
import com.example.domain.translation.MockTranslationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches QuickTranslate`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QuickTranslate", appName)
    }

    @Test
    fun `app is 100 percent free with unlimited entitlements and no paywalls`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val billingRepo = BillingRepository(context)
        val entitlements = billingRepo.getEntitlements()
        assertTrue(entitlements.isCompletelyFree)
        assertTrue(entitlements.unlimitedTranslations)
        assertTrue(entitlements.businessModeUnlocked)
        assertTrue(entitlements.floatingBubbleUnlocked)
    }

    @Test
    fun `user preferences repository manages floating bubble state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesRepository(context)
        assertFalse(prefs.floatingBubbleEnabled.value)
        prefs.setFloatingBubbleEnabled(true)
        assertTrue(prefs.floatingBubbleEnabled.value)
    }

    @Test
    fun `language detector detects Arabic, Hindi, Nepali, and French correctly`() {
        // Arabic test phrase
        val arabic = LanguageDetector.detectLanguage("مرحبا، كيف حالك؟")
        assertEquals("ar", arabic.code)

        // Hindi test phrase
        val hindi = LanguageDetector.detectLanguage("आप कैसे हैं?")
        assertEquals("hi", hindi.code)

        // Nepali test phrase
        val nepali = LanguageDetector.detectLanguage("तपाईंलाई कस्तो छ?")
        assertEquals("ne", nepali.code)

        // French test phrase
        val french = LanguageDetector.detectLanguage("Bonjour, comment allez-vous ?")
        assertEquals("fr", french.code)
    }

    @Test
    fun `business translator preserves commercial values accurately`() {
        val quotation = """
            MOQ 500 PCS
            Price USD 0.075/PC
            FOB Ningbo
            Payment 30% advance
        """.trimIndent()

        val protected = BusinessTranslator.protectTokens(quotation)
        assertTrue(protected.tokenMap.isNotEmpty())
        assertTrue(protected.extractedEntities.any { it.contains("500 PCS") })
        assertTrue(protected.extractedEntities.any { it.contains("0.075") })
        assertTrue(protected.extractedEntities.any { it.contains("FOB Ningbo") })
        assertTrue(protected.extractedEntities.any { it.contains("30%") })

        // Restore simulation
        val restored = BusinessTranslator.restoreTokens(protected.maskedText, protected.tokenMap)
        assertEquals(quotation, restored)
    }

    @Test
    fun `mock translation provider translates Arabic greeting accurately`() = runBlocking {
        val provider = MockTranslationProvider()
        val result = provider.translate(
            text = "مرحبا، كيف حالك؟",
            sourceLang = Language.ARABIC,
            targetLang = Language.ENGLISH,
            isBusinessMode = false
        )

        assertEquals("Hello, how are you?", result.translatedText)
        assertEquals(Language.ARABIC, result.detectedSourceLanguage)
        assertEquals(Language.ENGLISH, result.targetLanguage)
    }

    @Test
    fun `mock translation provider handles business commercial quotation`() = runBlocking {
        val provider = MockTranslationProvider()
        val quotation = "MOQ 500 PCS\nPrice USD 0.075/PC\nFOB Ningbo\nPayment 30% advance"
        val result = provider.translate(
            text = quotation,
            sourceLang = Language.ENGLISH,
            targetLang = Language.ARABIC,
            isBusinessMode = true
        )

        assertTrue(result.translatedText.contains("500 PCS"))
        assertTrue(result.translatedText.contains("USD 0.075/PC"))
        assertTrue(result.translatedText.contains("FOB Ningbo"))
        assertTrue(result.translatedText.contains("30%"))
        assertTrue(result.isBusinessModeApplied)
    }
}
