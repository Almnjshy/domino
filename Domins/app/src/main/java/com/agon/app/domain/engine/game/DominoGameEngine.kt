package com.agon.app.domain.engine.game

import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.BoardState
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameAction
import com.agon.app.domain.model.GameMode
import com.agon.app.domain.model.GameResult
import com.agon.app.domain.model.GameState
import com.agon.app.domain.model.Player
import com.agon.app.domain.validation.GameValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin game engine - single source of truth for game logic
 * No Android dependencies, fully testable
 */
@Singleton
class DominoGameEngine @Inject constructor() {

    private var currentState: GameState = GameState()
    private var matchStartTime: Long = 0
    private val validator = GameValidator()

    /**
     * Initialize a new game
     */
    fun newGame(mode: GameMode): GameState {
        matchStartTime = System.currentTimeMillis()
        val deck = DominoTile.createDeck()
        val playerCount = mode.playerCount
        val tilesPerPlayer = 28 / playerCount

        val players = List(playerCount) { index ->
            val isAi = index >= (playerCount - mode.aiCount)
            Player(
                id = index,
                name = if (isAi) "Bot ${index + 1}" else "Player ${index + 1}",
                isAi = isAi,
                hand = deck.drop(index * tilesPerPlayer).take(tilesPerPlayer)
            )
        }

        val remainingDeck = deck.drop(playerCount * tilesPerPlayer)
        val startingPlayer = findStartingPlayer(players)

        currentState = GameState(
            players = players,
            board = BoardState(),
            stock = remainingDeck,
            currentPlayerIndex = startingPlayer,
            gameMode = mode,
            message = "${players[startingPlayer].displayName()} يبدأ اللعب"
        )

        return currentState
    }

    /**
     * Play a tile on the board
     */
    fun playTile(tile: DominoTile, side: BoardSide): Result<GameState> {
        val validation = validator.validatePlayTile(currentState, tile, side)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        val currentPlayer = currentState.currentPlayer!!
        val newBoard = currentState.board.place(tile, side)
        val updatedPlayer = currentPlayer.withoutTile(tile)

        val updatedPlayers = currentState.players.map {
            if (it.id == currentPlayer.id) updatedPlayer else it
        }

        var newState = currentState.copy(
            players = updatedPlayers,
            board = newBoard,
            lastAction = GameAction.PlayTile(currentPlayer.id, tile, side)
        )

        // Check win conditions
        newState = checkWinConditions(newState, updatedPlayer)

        if (!newState.roundOver && !newState.isBlocked) {
            newState = newState.nextPlayer()
                .withMessage("دور ${newState.currentPlayer?.displayName()}")
        }

        currentState = newState
        return Result.success(newState)
    }

    /**
     * Draw a tile or pass turn
     */
    fun drawOrPass(): Result<GameState> {
        val currentPlayer = currentState.currentPlayer 
            ?: return Result.failure(IllegalStateException("No current player"))

        if (currentState.stock.isEmpty()) {
            // Pass turn
            val newState = currentState.nextPlayer()
                .withMessage("${currentPlayer.displayName()} تخطى")
                .copy(lastAction = GameAction.PassTurn(currentPlayer.id))

            currentState = newState
            return Result.success(newState)
        }

        // Draw tile
        val drawnTile = currentState.stock.first()
        val newStock = currentState.stock.drop(1)
        val updatedPlayer = currentPlayer.withTile(drawnTile)

        val updatedPlayers = currentState.players.map {
            if (it.id == currentPlayer.id) updatedPlayer else it
        }

        var newState = currentState.copy(
            players = updatedPlayers,
            stock = newStock,
            lastAction = GameAction.DrawTile(currentPlayer.id, drawnTile)
        )

        // Check if drawn tile can be played
        if (getLegalSides(drawnTile).isNotEmpty()) {
            newState = newState.withMessage(
                "${currentPlayer.displayName()} سحب قطعة ويمكن لعبها"
            )
        } else {
            newState = newState.nextPlayer()
                .withMessage("${currentPlayer.displayName()} سحب ومرر الدور")
        }

        currentState = newState
        return Result.success(newState)
    }

    /**
     * Get legal sides for a tile
     */
    fun getLegalSides(tile: DominoTile): Set<BoardSide> {
        return currentState.board.getLegalSides(tile)
    }

    /**
     * Check if current player has legal moves
     */
    fun hasLegalMoves(): Boolean {
        val currentPlayer = currentState.currentPlayer ?: return false
        return currentPlayer.hand.any { getLegalSides(it).isNotEmpty() }
    }

    /**
     * Check if game is over
     */
    fun isGameOver(): Boolean {
        return currentState.roundOver || currentState.isBlocked
    }

    /**
     * Get winner ID
     */
    fun getWinner(): Int? {
        return currentState.winnerId
    }

    /**
     * Get current scores
     */
    fun getScores(): Map<Int, Int> {
        return currentState.players.associate { it.id to it.handValue }
    }

    /**
     * Get current state
     */
    fun getCurrentState(): GameState = currentState

    /**
     * Get game result if game is over
     */
    fun getGameResult(): GameResult? {
        if (!isGameOver()) return null

        val duration = (System.currentTimeMillis() - matchStartTime) / 1000
        val winner = currentState.winnerId
        val winnerName = winner?.let { currentState.players.getOrNull(it)?.displayName() } ?: "تعادل"

        return GameResult(
            winnerId = winner ?: -1,
            winnerName = winnerName,
            scores = getScores(),
            durationSeconds = duration,
            gameMode = currentState.gameMode
        )
    }

    /**
     * Reset game
     */
    fun resetGame(): GameState {
        return newGame(currentState.gameMode)
    }

    // Private helpers
    private fun findStartingPlayer(players: List<Player>): Int {
        var bestPlayer = 0
        var bestValue = -1

        players.forEachIndexed { index, player ->
            val highestDouble = player.hand.filter { it.isDouble }.maxByOrNull { it.total }
            val value = highestDouble?.total ?: player.hand.maxOf { it.total }

            if (highestDouble != null && value > bestValue) {
                bestValue = value
                bestPlayer = index
            } else if (highestDouble == null && value > bestValue) {
                bestValue = value
                bestPlayer = index
            }
        }

        return bestPlayer
    }

    private fun checkWinConditions(state: GameState, lastPlayer: Player): GameState {
        var newState = state

        // Check if player emptied hand
        if (lastPlayer.hand.isEmpty()) {
            val scores = getScores()
            newState = newState.copy(
                roundOver = true,
                winnerId = lastPlayer.id,
                message = "${lastPlayer.displayName()} فاز بالجولة!",
                lastAction = GameAction.WinRound(lastPlayer.id, scores)
            )
            return newState
        }

        // Check if game is blocked
        if (isGameBlocked(newState)) {
            val winner = determineBlockedWinner(newState.players)
            val scores = getScores()
            newState = newState.copy(
                roundOver = true,
                isBlocked = true,
                winnerId = winner,
                message = "اللعبة متوقفة! ${winner?.let { newState.players[it].displayName() } ?: "تعادل"}",
                lastAction = GameAction.WinRound(winner ?: -1, scores)
            )
        }

        return newState
    }

    private fun isGameBlocked(state: GameState): Boolean {
        if (state.stock.isNotEmpty()) return false
        return state.players.all { player ->
            player.hand.all { getLegalSides(it).isEmpty() }
        }
    }

    private fun determineBlockedWinner(players: List<Player>): Int? {
        val handValues = players.map { it.id to it.handValue }
        val minValue = handValues.minByOrNull { it.second }?.second ?: return null
        val winners = handValues.filter { it.second == minValue }

        return if (winners.size == 1) winners.first().first else null
    }
}
