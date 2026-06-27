package com.agon.app.domain.engine.ai

import com.agon.app.domain.model.AiDifficulty
import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameState
import com.agon.app.domain.model.Player
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Pure Kotlin AI Engine for Domino game
 * No Android dependencies, fully testable
 */
@Singleton
class DominoAIEngine @Inject constructor() {

    data class AIMove(
        val tile: DominoTile,
        val side: BoardSide,
        val confidence: Float,
        val reasoning: MoveReasoning
    )

    data class MoveReasoning(
        val score: Float,
        val isDouble: Boolean,
        val blocksOpponent: Boolean,
        val leavesGoodOptions: Boolean,
        val riskLevel: RiskLevel
    )

    enum class RiskLevel { LOW, MEDIUM, HIGH }

    /**
     * Calculate best move for AI player
     */
    fun calculateBestMove(
        gameState: GameState,
        player: Player,
        difficulty: AiDifficulty
    ): AIMove? {
        val legalMoves = findAllLegalMoves(gameState, player)

        if (legalMoves.isEmpty()) return null

        // Apply difficulty-based mistake probability
        if (Random.nextFloat() < difficulty.mistakeProbability) {
            return makeMistake(legalMoves)
        }

        return when (difficulty) {
            AiDifficulty.EASY -> selectEasyMove(legalMoves)
            AiDifficulty.MEDIUM -> selectMediumMove(legalMoves, gameState, player)
            AiDifficulty.HARD -> selectHardMove(legalMoves, gameState, player)
        }
    }

    /**
     * Evaluate all possible moves
     */
    fun evaluateMoves(
        gameState: GameState,
        player: Player
    ): List<AIMove> {
        return findAllLegalMoves(gameState, player)
    }

    /**
     * Check if AI should draw or pass
     */
    fun shouldDrawOrPass(gameState: GameState, player: Player): Boolean {
        return findAllLegalMoves(gameState, player).isEmpty()
    }

    private fun findAllLegalMoves(gameState: GameState, player: Player): List<AIMove> {
        return player.hand.flatMap { tile ->
            val sides = gameState.board.getLegalSides(tile)
            sides.map { side ->
                val reasoning = analyzeMove(tile, side, gameState, player)
                AIMove(tile, side, reasoning.score, reasoning)
            }
        }
    }

    private fun analyzeMove(
        tile: DominoTile,
        side: BoardSide,
        gameState: GameState,
        player: Player
    ): MoveReasoning {
        var score = tile.total.toFloat()

        // Double bonus
        val isDouble = tile.isDouble
        if (isDouble) score += 5f

        // Early game high-value bonus
        if (gameState.turnCount < 5 && tile.total > 8) score += 3f

        // End matching bonus
        val board = gameState.board
        val endValue = if (side == BoardSide.LEFT) board.leftEnd else board.rightEnd
        if (endValue != null && (tile.top == endValue || tile.bottom == endValue)) {
            score += 2f
        }

        // Check if move blocks opponent
        val remainingTiles = player.hand.filter { it.id != tile.id }
        val blocksOpponent = checkIfBlocksOpponent(tile, side, gameState, remainingTiles)
        if (blocksOpponent) score += 4f

        // Check future options
        val leavesGoodOptions = remainingTiles.any {
            gameState.board.getLegalSides(it).isNotEmpty()
        }
        if (leavesGoodOptions) score += 1f

        // Risk assessment
        val riskLevel = assessRisk(tile, remainingTiles, gameState)
        when (riskLevel) {
            RiskLevel.LOW -> score += 1f
            RiskLevel.HIGH -> score -= 2f
            else -> {} // No adjustment
        }

        return MoveReasoning(score, isDouble, blocksOpponent, leavesGoodOptions, riskLevel)
    }

    private fun checkIfBlocksOpponent(
        tile: DominoTile,
        side: BoardSide,
        gameState: GameState,
        remainingTiles: List<DominoTile>
    ): Boolean {
        // Simulate board after move
        val newBoard = gameState.board.place(tile, side)

        // Check if opponents have matching tiles
        val opponents = gameState.players.filter { it.id != gameState.currentPlayerIndex }
        return opponents.all { opponent ->
            opponent.hand.all { opponentTile ->
                newBoard.getLegalSides(opponentTile).isEmpty()
            }
        }
    }

    private fun assessRisk(
        tile: DominoTile,
        remainingTiles: List<DominoTile>,
        gameState: GameState
    ): RiskLevel {
        return when {
            remainingTiles.isEmpty() -> RiskLevel.LOW // Winning move
            remainingTiles.size <= 2 && remainingTiles.all { 
                gameState.board.getLegalSides(it).isEmpty() 
            } -> RiskLevel.HIGH // Might get stuck
            tile.total >= 10 -> RiskLevel.MEDIUM // High value tile
            else -> RiskLevel.LOW
        }
    }

    private fun makeMistake(moves: List<AIMove>): AIMove {
        // Pick a suboptimal move
        val sorted = moves.sortedByDescending { it.confidence }
        return if (sorted.size > 2 && Random.nextBoolean()) {
            sorted[Random.nextInt(1, sorted.size)]
        } else {
            sorted.random()
        }
    }

    private fun selectEasyMove(moves: List<AIMove>): AIMove {
        // Prefer low-value moves, random selection
        return moves.minByOrNull { it.confidence + Random.nextFloat() * 3f } 
            ?: moves.random()
    }

    private fun selectMediumMove(
        moves: List<AIMove>,
        gameState: GameState,
        player: Player
    ): AIMove {
        val sorted = moves.sortedByDescending { it.confidence }

        // 70% pick best, 30% pick second best
        return if (Random.nextFloat() < 0.7f) {
            sorted.first()
        } else {
            sorted.getOrNull(1) ?: sorted.first()
        }
    }

    private fun selectHardMove(
        moves: List<AIMove>,
        gameState: GameState,
        player: Player
    ): AIMove {
        val bestMove = moves.maxByOrNull { it.confidence } ?: return moves.first()

        // Lookahead: prefer moves that leave good options
        val remainingTiles = player.hand.filter { it.id != bestMove.tile.id }
        val futureOptions = remainingTiles.count {
            gameState.board.getLegalSides(it).isNotEmpty()
        }

        return if (futureOptions == 0 && moves.size > 1) {
            // Don't play the only tile that leaves options if possible
            moves.filter { it.tile.id != bestMove.tile.id }
                .maxByOrNull { it.confidence } ?: bestMove
        } else {
            bestMove
        }
    }
}
