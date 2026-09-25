package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceLangCode: String,
    val sourceLangName: String,
    val originalText: String,
    val targetLangCode: String,
    val targetLangName: String,
    val translatedText: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)
