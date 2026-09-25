package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlanEntitlements(
    val isCompletelyFree: Boolean = true,
    val unlimitedTranslations: Boolean = true,
    val businessModeUnlocked: Boolean = true,
    val advancedAiTranslation: Boolean = true,
    val prioritySpeed: Boolean = true,
    val adFree: Boolean = true,
    val floatingBubbleUnlocked: Boolean = true
)

class BillingRepository(context: Context) {

    // 100% Free Forever - all capabilities permanently unlocked
    private val _isProSubscribed = MutableStateFlow(true)
    val isProSubscribed: StateFlow<Boolean> = _isProSubscribed.asStateFlow()

    fun getEntitlements(): PlanEntitlements {
        return PlanEntitlements()
    }
}
