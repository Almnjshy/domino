package com.agon.app.domain.usecase.ai

import com.agon.app.domain.engine.ai.DominoAIEngine
import com.agon.app.domain.model.AiDifficulty
import com.agon.app.domain.model.GameState
import com.agon.app.domain.model.Player
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * AI Play UseCase with delay handling
 * Delay is here, not in ViewModel
 */
class AIPlayUseCase @Inject constructor(
    private val aiEngine: DominoAIEngine
) {
    suspend operator fun invoke(
        gameState: GameState,
        player: Player,
        difficulty: AiDifficulty
    ): DominoAIEngine.AIMove? {
        // Apply AI thinking delay based on difficulty
        delay(difficulty.delayMs)

        return aiEngine.calculateBestMove(gameState, player, difficulty)
    }

    fun shouldDrawOrPass(gameState: GameState, player: Player): Boolean {
        return aiEngine.shouldDrawOrPass(gameState, player)
    }

    fun evaluateMoves(gameState: GameState, player: Player): List<DominoAIEngine.AIMove> {
        return aiEngine.evaluateMoves(gameState, player)
    }
}
