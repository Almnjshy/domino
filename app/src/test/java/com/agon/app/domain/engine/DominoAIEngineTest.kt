package com.agon.app.domain.engine

import com.agon.app.domain.engine.ai.DominoAIEngine
import com.agon.app.domain.model.AiDifficulty
import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameMode
import com.agon.app.domain.model.GameState
import com.agon.app.domain.model.Player
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DominoAIEngineTest {

    private lateinit var aiEngine: DominoAIEngine
    private lateinit var gameEngine: com.agon.app.domain.engine.game.DominoGameEngine

    @Before
    fun setup() {
        aiEngine = DominoAIEngine()
        gameEngine = com.agon.app.domain.engine.game.DominoGameEngine()
    }

    @Test
    fun `calculateBestMove returns valid move`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val aiPlayer = state.players.first { it.isAi }

        val move = aiEngine.calculateBestMove(state, aiPlayer, AiDifficulty.MEDIUM)

        assertNotNull(move)
        assertTrue(aiPlayer.hand.any { it.id == move!!.tile.id })
        assertTrue(move!!.side == BoardSide.LEFT || move.side == BoardSide.RIGHT)
    }

    @Test
    fun `calculateBestMove returns null when no legal moves`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val player = state.players.first()
        // Empty hand = no moves
        val emptyPlayer = player.copy(hand = emptyList())

        val move = aiEngine.calculateBestMove(state, emptyPlayer, AiDifficulty.HARD)

        assertNull(move)
    }

    @Test
    fun `easy difficulty makes mistakes sometimes`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val aiPlayer = state.players.first { it.isAi }

        // Run multiple times to check for mistakes
        var differentMoves = 0
        val firstMove = aiEngine.calculateBestMove(state, aiPlayer, AiDifficulty.EASY)

        repeat(10) {
            val move = aiEngine.calculateBestMove(state, aiPlayer, AiDifficulty.EASY)
            if (move?.tile?.id != firstMove?.tile?.id || move?.side != firstMove?.side) {
                differentMoves++
            }
        }

        // Easy should have some variation due to mistakes
        assertTrue("Easy AI should make some mistakes", differentMoves > 0)
    }

    @Test
    fun `hard difficulty is consistent`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val aiPlayer = state.players.first { it.isAi }

        val moves = List(5) {
            aiEngine.calculateBestMove(state, aiPlayer, AiDifficulty.HARD)
        }

        // Hard should be more consistent
        val uniqueMoves = moves.distinctBy { it?.tile?.id to it?.side }
        assertTrue("Hard AI should be consistent", uniqueMoves.size <= 2)
    }

    @Test
    fun `evaluateMoves returns all legal moves`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val aiPlayer = state.players.first { it.isAi }

        val moves = aiEngine.evaluateMoves(state, aiPlayer)

        assertTrue(moves.isNotEmpty())
        moves.forEach { move ->
            assertTrue(aiPlayer.hand.any { it.id == move.tile.id })
        }
    }

    @Test
    fun `shouldDrawOrPass when no legal moves`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val player = state.players.first().copy(hand = emptyList())

        assertTrue(aiEngine.shouldDrawOrPass(state, player))
    }

    @Test
    fun `move reasoning contains valid data`() {
        val state = gameEngine.newGame(GameMode.HUMAN_VS_AI)
        val aiPlayer = state.players.first { it.isAi }

        val move = aiEngine.calculateBestMove(state, aiPlayer, AiDifficulty.HARD)

        assertNotNull(move)
        assertTrue(move!!.confidence >= 0f)
        assertNotNull(move.reasoning)
    }
}
