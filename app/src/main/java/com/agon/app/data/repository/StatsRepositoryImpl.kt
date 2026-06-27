package com.agon.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.agon.app.domain.model.GameResult
import com.agon.app.domain.model.MatchRecord
import com.agon.app.domain.model.StatsData
import com.agon.app.domain.repository.Achievement
import com.agon.app.domain.repository.StatsRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StatsRepository using SharedPreferences + Gson
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : StatsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(STATS_PREFS, Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow(loadFromPrefs())

    companion object {
        private const val STATS_PREFS = "domino_stats"
        private const val KEY_STATS_DATA = "stats_data"
        private const val KEY_ACHIEVEMENTS = "achievements"
    }

    override val stats: Flow<StatsData> = _stats.asStateFlow()

    override suspend fun loadStats(): StatsData {
        return loadFromPrefs()
    }

    override suspend fun recordGame(result: GameResult, playerId: Int): Result<StatsData> {
        return try {
            val currentStats = _stats.value
            val isWinner = result.winnerId == playerId
            val playerScore = result.scores[playerId] ?: 0

            val updatedStats = currentStats.recordMatch(result, isWinner)

            saveToPrefs(updatedStats)
            _stats.value = updatedStats

            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearStats(): Result<Unit> {
        return try {
            prefs.edit().clear().apply()
            _stats.value = StatsData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToJson(): Result<String> {
        return try {
            val json = gson.toJson(_stats.value)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importFromJson(json: String): Result<StatsData> {
        return try {
            val stats = gson.fromJson(json, StatsData::class.java)
            saveToPrefs(stats)
            _stats.value = stats
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAchievements(): List<Achievement> {
        val stats = _stats.value
        return listOf(
            Achievement(
                id = "first_win",
                title = "أول فوز",
                description = "فز في أول مباراة",
                icon = "trophy",
                isUnlocked = stats.wins >= 1,
                progress = minOf(stats.wins.toFloat(), 1f),
                unlockedAt = if (stats.wins >= 1) System.currentTimeMillis() else null
            ),
            Achievement(
                id = "win_streak_5",
                title = "سلسلة فوز",
                description = "فز 5 مرات متتالية",
                icon = "fire",
                isUnlocked = stats.longestWinStreak >= 5,
                progress = minOf(stats.longestWinStreak.toFloat() / 5f, 1f),
                unlockedAt = if (stats.longestWinStreak >= 5) System.currentTimeMillis() else null
            ),
            Achievement(
                id = "play_10",
                title = "لاعب نشط",
                description = "لعب 10 مباريات",
                icon = "games",
                isUnlocked = stats.matchesPlayed >= 10,
                progress = minOf(stats.matchesPlayed.toFloat() / 10f, 1f),
                unlockedAt = if (stats.matchesPlayed >= 10) System.currentTimeMillis() else null
            ),
            Achievement(
                id = "play_100",
                title = "محترف الدومينو",
                description = "لعب 100 مباراة",
                icon = "star",
                isUnlocked = stats.matchesPlayed >= 100,
                progress = minOf(stats.matchesPlayed.toFloat() / 100f, 1f),
                unlockedAt = if (stats.matchesPlayed >= 100) System.currentTimeMillis() else null
            ),
            Achievement(
                id = "win_rate_50",
                title = "فائز محترف",
                description = "نسبة فوز 50%",
                icon = "emoji_events",
                isUnlocked = stats.winRate >= 50f && stats.matchesPlayed >= 10,
                progress = if (stats.matchesPlayed >= 10) minOf(stats.winRate / 50f, 1f) else 0f,
                unlockedAt = if (stats.winRate >= 50f && stats.matchesPlayed >= 10) System.currentTimeMillis() else null
            )
        )
    }

    override fun observeStats(): Flow<StatsData> = stats

    private fun loadFromPrefs(): StatsData {
        val json = prefs.getString(KEY_STATS_DATA, null)
        return if (json != null) {
            try {
                gson.fromJson(json, StatsData::class.java)
            } catch (e: Exception) {
                StatsData()
            }
        } else {
            StatsData()
        }
    }

    private fun saveToPrefs(stats: StatsData) {
        val json = gson.toJson(stats)
        prefs.edit().putString(KEY_STATS_DATA, json).apply()
    }
}
