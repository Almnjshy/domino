package com.agon.app.domain.repository

import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameMode
import com.agon.app.domain.model.GameResult
import com.agon.app.domain.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for game operations
 */
interface GameRepository {

    /** Current game state */
    val gameState: StateFlow<GameState>

    /** Stream of game results */
    val gameResults: Flow<GameResult>

    /**
     * Initialize a new game with specified mode
     */
    suspend fun newGame(mode: GameMode): Result<GameState>

    /**
     * Play a tile on the board
     */
    suspend fun playTile(tile: DominoTile, side: BoardSide): Result<GameState>

    /**
     * Draw a tile from stock or pass turn
     */
    suspend fun drawOrPass(): Result<GameState>

    /**
     * Get legal sides for a tile
     */
    fun getLegalSides(tile: DominoTile): Set<BoardSide>

    /**
     * Check if current player has legal moves
     */
    fun hasLegalMoves(): Boolean

    /**
     * Check if game is over
     */
    fun isGameOver(): Boolean

    /**
     * Get the winner if game is over
     */
    fun getWinner(): Int?

    /**
     * Reset the game
     */
    suspend fun resetGame(): Result<GameState>

    /**
     * Get current scores
     */
    fun getScores(): Map<Int, Int>
}
