package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.TranslationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    // --- History Queries ---
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TranslationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: TranslationHistoryEntity): Long

    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearAllHistory()

    // --- Favorites Queries ---
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(item: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE originalText = :originalText AND targetLangCode = :targetLangCode)")
    suspend fun isFavorite(originalText: String, targetLangCode: String): Boolean

    @Query("DELETE FROM favorites WHERE originalText = :originalText AND targetLangCode = :targetLangCode")
    suspend fun removeFavoriteByText(originalText: String, targetLangCode: String)
}
