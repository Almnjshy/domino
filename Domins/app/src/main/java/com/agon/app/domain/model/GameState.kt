package com.agon.app.domain.model

/**
 * Complete game state representing the entire game
 */
data class GameState(
    val players: List<Player> = emptyList(),
    val board: BoardState = BoardState(),
    val stock: List<DominoTile> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val roundOver: Boolean = false,
    val winnerId: Int? = null,
    val isBlocked: Boolean = false,
    val message: String = "",
    val turnCount: Int = 0,
    val gameMode: GameMode = GameMode.HUMAN_VS_AI,
    val lastAction: GameAction? = null
) {
    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
    val isGameOver: Boolean get() = roundOver || isBlocked
    val stockCount: Int get() = stock.size
    val canDraw: Boolean get() = stock.isNotEmpty()

    fun nextPlayer(): GameState {
        if (players.isEmpty()) return this
        val nextIndex = (currentPlayerIndex + 1) % players.size
        return copy(currentPlayerIndex = nextIndex, turnCount = turnCount + 1)
    }

    fun withMessage(msg: String): GameState = copy(message = msg)

    fun withPlayerUpdated(playerId: Int, update: (Player) -> Player): GameState {
        val updatedPlayers = players.map { 
            if (it.id == playerId) update(it) else it 
        }
        return copy(players = updatedPlayers)
    }
}

sealed class GameAction {
    data class PlayTile(val playerId: Int, val tile: DominoTile, val side: BoardSide) : GameAction()
    data class DrawTile(val playerId: Int, val tile: DominoTile?) : GameAction()
    data class PassTurn(val playerId: Int) : GameAction()
    data class WinRound(val winnerId: Int, val scores: Map<Int, Int>) : GameAction()
}
