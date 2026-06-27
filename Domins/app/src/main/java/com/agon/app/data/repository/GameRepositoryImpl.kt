package com.agon.app.data.repository

import com.agon.app.domain.engine.ai.DominoAIEngine
import com.agon.app.domain.engine.game.DominoGameEngine
import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameMode
import com.agon.app.domain.model.GameResult
import com.agon.app.domain.model.GameState
import com.agon.app.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GameRepository implementation using DominoGameEngine
 * Delegates all game logic to the engine
 */
@Singleton
class GameRepositoryImpl @Inject constructor(
    private val gameEngine: DominoGameEngine,
    private val aiEngine: DominoAIEngine
) : GameRepository {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _gameResults = MutableStateFlow<GameResult?>(null)
    override val gameResults: Flow<GameResult> = _gameResults.filterNotNull()

    override suspend fun newGame(mode: GameMode): Result<GameState> {
        return try {
            val state = gameEngine.newGame(mode)
            _gameState.value = state
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun playTile(tile: DominoTile, side: BoardSide): Result<GameState> {
        return gameEngine.playTile(tile, side).onSuccess { state ->
            _gameState.value = state
            checkAndEmitResult()
        }
    }

    override suspend fun drawOrPass(): Result<GameState> {
        return gameEngine.drawOrPass().onSuccess { state ->
            _gameState.value = state
            checkAndEmitResult()
        }
    }

    override fun getLegalSides(tile: DominoTile): Set<BoardSide> {
        return gameEngine.getLegalSides(tile)
    }

    override fun hasLegalMoves(): Boolean {
        return gameEngine.hasLegalMoves()
    }

    override fun isGameOver(): Boolean {
        return gameEngine.isGameOver()
    }

    override fun getWinner(): Int? {
        return gameEngine.getWinner()
    }

    override suspend fun resetGame(): Result<GameState> {
        return try {
            val state = gameEngine.resetGame()
            _gameState.value = state
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getScores(): Map<Int, Int> {
        return gameEngine.getScores()
    }

    /**
     * Calculate AI move using DominoAIEngine
     */
    fun calculateAIMove(
        state: GameState,
        player: com.agon.app.domain.model.Player,
        difficulty: com.agon.app.domain.model.AiDifficulty
    ): DominoAIEngine.AIMove? {
        return aiEngine.calculateBestMove(state, player, difficulty)
    }

    /**
     * Check if AI should draw or pass
     */
    fun shouldAIDrawOrPass(state: GameState, player: com.agon.app.domain.model.Player): Boolean {
        return aiEngine.shouldDrawOrPass(state, player)
    }

    private fun checkAndEmitResult() {
        val result = gameEngine.getGameResult()
        if (result != null) {
            _gameResults.value = result
        }
    }
}
