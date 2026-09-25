package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceLangCode: String,
    val sourceLangName: String,
    val originalText: String,
    val targetLangCode: String,
    val targetLangName: String,
    val translatedText: String,
    val isBusinessMode: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
