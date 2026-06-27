package com.agon.app.domain.repository

import com.agon.app.domain.model.GameResult
import com.agon.app.domain.model.StatsData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for statistics operations
 */
interface StatsRepository {

    /** Current stats */
    val stats: Flow<StatsData>

    /**
     * Load stats from storage
     */
    suspend fun loadStats(): StatsData

    /**
     * Record a completed game
     */
    suspend fun recordGame(result: GameResult, playerId: Int): Result<StatsData>

    /**
     * Clear all statistics
     */
    suspend fun clearStats(): Result<Unit>

    /**
     * Export stats to JSON
     */
    suspend fun exportToJson(): Result<String>

    /**
     * Import stats from JSON
     */
    suspend fun importFromJson(json: String): Result<StatsData>

    /**
     * Get achievements
     */
    suspend fun getAchievements(): List<Achievement>

    /**
     * Observe stats changes
     */
    fun observeStats(): Flow<StatsData>
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float,
    val unlockedAt: Long? = null
)
