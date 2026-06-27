package com.agon.app.domain.engine

import com.agon.app.domain.engine.game.DominoGameEngine
import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DominoGameEngineTest {

    private lateinit var engine: DominoGameEngine

    @Before
    fun setup() {
        engine = DominoGameEngine()
    }

    @Test
    fun `newGame creates valid game state`() {
        val state = engine.newGame(GameMode.HUMAN_VS_AI)

        assertEquals(2, state.players.size)
        assertEquals(7, state.players[0].hand.size)
        assertEquals(7, state.players[1].hand.size)
        assertEquals(14, state.stock.size)
        assertFalse(state.roundOver)
        assertFalse(state.isBlocked)
    }

    @Test
    fun `newGame with four players`() {
        val state = engine.newGame(GameMode.FOUR_HUMANS)

        assertEquals(4, state.players.size)
        assertEquals(7, state.players[0].hand.size)
        assertEquals(0, state.stock.size)
    }

    @Test
    fun `playTile removes tile from hand`() = runTest {
        engine.newGame(GameMode.HUMAN_VS_AI)
        val player = engine.getCurrentState().currentPlayer!!
        val tile = player.hand.first()
        val side = engine.getLegalSides(tile).firstOrNull() ?: BoardSide.LEFT

        val result = engine.playTile(tile, side)

        assertTrue(result.isSuccess)
        val newState = result.getOrNull()!!
        assertFalse(newState.players[player.id].hand.any { it.id == tile.id })
    }

    @Test
    fun `playTile with invalid tile fails`() = runTest {
        engine.newGame(GameMode.HUMAN_VS_AI)
        val invalidTile = DominoTile(99, 99)

        val result = engine.playTile(invalidTile, BoardSide.LEFT)

        assertTrue(result.isFailure)
    }

    @Test
    fun `drawOrPass reduces stock`() = runTest {
        engine.newGame(GameMode.HUMAN_VS_AI)
        val initialStock = engine.getCurrentState().stock.size

        val result = engine.drawOrPass()

        assertTrue(result.isSuccess)
        val newState = result.getOrNull()!!
        assertTrue(newState.stock.size < initialStock || newState.currentPlayerIndex != 0)
    }

    @Test
    fun `getLegalSides returns valid sides`() {
        engine.newGame(GameMode.HUMAN_VS_AI)
        val player = engine.getCurrentState().currentPlayer!!
        val tile = player.hand.first()

        val sides = engine.getLegalSides(tile)

        // First move should allow both sides
        assertTrue(sides.isNotEmpty())
    }

    @Test
    fun `isGameOver returns false for new game`() {
        engine.newGame(GameMode.HUMAN_VS_AI)
        assertFalse(engine.isGameOver())
    }

    @Test
    fun `getScores returns player hand values`() {
        engine.newGame(GameMode.HUMAN_VS_AI)
        val scores = engine.getScores()

        assertEquals(2, scores.size)
        assertTrue(scores.values.all { it >= 0 })
    }

    @Test
    fun `resetGame creates new game with same mode`() {
        engine.newGame(GameMode.HUMAN_VS_AI)
        val state = engine.resetGame()

        assertEquals(GameMode.HUMAN_VS_AI, state.gameMode)
        assertFalse(state.roundOver)
    }

    @Test
    fun `tile count consistency`() {
        val state = engine.newGame(GameMode.HUMAN_VS_AI)
        val totalTiles = state.players.sumOf { it.hand.size } + 
                        state.stock.size + 
                        state.board.tiles.size

        assertEquals(28, totalTiles)
    }
}
