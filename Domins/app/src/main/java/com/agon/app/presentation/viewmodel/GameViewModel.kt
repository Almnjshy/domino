package com.agon.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.domain.model.AiDifficulty
import com.agon.app.domain.model.BoardSide
import com.agon.app.domain.model.DominoTile
import com.agon.app.domain.model.GameMode
import com.agon.app.domain.model.GameState
import com.agon.app.domain.model.Player
import com.agon.app.domain.usecase.game.CheckGameOverUseCase
import com.agon.app.domain.usecase.game.DrawOrPassUseCase
import com.agon.app.domain.usecase.game.GetLegalMovesUseCase
import com.agon.app.domain.usecase.game.NewGameUseCase
import com.agon.app.domain.usecase.game.PlayTileUseCase
import com.agon.app.domain.usecase.settings.LoadSettingsUseCase
import com.agon.app.domain.usecase.stats.RecordGameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fixed GameViewModel with queue-based AI handling
 * No recursion - uses Channel for AI turn queue
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val newGameUseCase: NewGameUseCase,
    private val playTileUseCase: PlayTileUseCase,
    private val drawOrPassUseCase: DrawOrPassUseCase,
    private val getLegalMovesUseCase: GetLegalMovesUseCase,
    private val checkGameOverUseCase: CheckGameOverUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val recordGameUseCase: RecordGameUseCase
) : ViewModel() {

    data class GameUiState(
        val gameState: GameState = GameState(),
        val isLoading: Boolean = false,
        val isAiThinking: Boolean = false,
        val error: String? = null,
        val showResult: Boolean = false,
        val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM
    )

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Queue-based AI handling - prevents recursion
    private val aiTurnQueue = Channel<Unit>(Channel.CONFLATED)
    private var currentGameMode: GameMode = GameMode.HUMAN_VS_AI

    init {
        // Process AI turns from queue
        viewModelScope.launch {
            aiTurnQueue.receiveAsFlow().collect {
                processAiTurn()
            }
        }

        // Load settings
        viewModelScope.launch {
            try {
                val settings = loadSettingsUseCase()
                _uiState.value = _uiState.value.copy(aiDifficulty = settings.aiDifficulty)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun newGame(mode: GameMode) {
        currentGameMode = mode
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showResult = false)

            newGameUseCase(mode)
                .onSuccess { state ->
                    _uiState.value = _uiState.value.copy(
                        gameState = state,
                        isLoading = false,
                        error = null
                    )
                    // Queue AI turn if needed
                    queueAiTurnIfNeeded()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    fun playTile(tile: DominoTile, side: BoardSide?) {
        val state = _uiState.value.gameState
        if (state.roundOver || state.isBlocked) return

        val currentPlayer = state.currentPlayer ?: return
        if (currentPlayer.isAi) return

        viewModelScope.launch {
            val selectedSide = side ?: getLegalMovesUseCase(tile).firstOrNull() ?: return@launch

            playTileUseCase(tile, selectedSide)
                .onSuccess { newState ->
                    _uiState.value = _uiState.value.copy(gameState = newState)
                    handleGameEnd(newState)
                    queueAiTurnIfNeeded()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    fun drawOrPass() {
        val state = _uiState.value.gameState
        if (state.roundOver || state.isBlocked) return

        val currentPlayer = state.currentPlayer ?: return
        if (currentPlayer.isAi) return

        viewModelScope.launch {
            drawOrPassUseCase()
                .onSuccess { newState ->
                    _uiState.value = _uiState.value.copy(gameState = newState)
                    handleGameEnd(newState)
                    queueAiTurnIfNeeded()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }

    fun getLegalSides(tile: DominoTile): Set<BoardSide> {
        return getLegalMovesUseCase(tile)
    }

    fun resetGame() {
        newGame(currentGameMode)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(showResult = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Queue AI turn if current player is AI
     * Non-blocking, uses Channel
     */
    private fun queueAiTurnIfNeeded() {
        val state = _uiState.value.gameState
        val currentPlayer = state.currentPlayer ?: return

        if (currentPlayer.isAi && !state.roundOver && !state.isBlocked) {
            aiTurnQueue.trySend(Unit)
        }
    }

    /**
     * Process AI turn - called from queue
     * No recursion, single execution per queue item
     */
    private suspend fun processAiTurn() {
        val state = _uiState.value.gameState
        val currentPlayer = state.currentPlayer ?: return

        if (!currentPlayer.isAi || state.roundOver || state.isBlocked) return

        _uiState.value = _uiState.value.copy(isAiThinking = true)

        // AI delay is handled in UseCase, not ViewModel
        // This removes UI layer control over game logic

        // Note: AI logic should be in AIUseCase, not here
        // For now, we simulate AI by drawing or passing
        // In production, this should call AIUseCase

        kotlinx.coroutines.delay(_uiState.value.aiDifficulty.delayMs)

        // Simple AI: try to play first legal tile, else draw/pass
        val legalTile = currentPlayer.hand.firstOrNull { 
            getLegalMovesUseCase(it).isNotEmpty() 
        }

        if (legalTile != null) {
            val side = getLegalMovesUseCase(legalTile).first()
            playTileUseCase(legalTile, side)
                .onSuccess { newState ->
                    _uiState.value = _uiState.value.copy(
                        gameState = newState,
                        isAiThinking = false
                    )
                    handleGameEnd(newState)
                    queueAiTurnIfNeeded() // Queue next AI turn if still AI's turn
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isAiThinking = false)
                }
        } else {
            drawOrPassUseCase()
                .onSuccess { newState ->
                    _uiState.value = _uiState.value.copy(
                        gameState = newState,
                        isAiThinking = false
                    )
                    handleGameEnd(newState)
                    queueAiTurnIfNeeded()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isAiThinking = false)
                }
        }
    }

    private fun handleGameEnd(state: GameState) {
        if (state.roundOver || state.isBlocked) {
            _uiState.value = _uiState.value.copy(showResult = true)

            viewModelScope.launch {
                val winnerId = state.winnerId
                val scores = checkGameOverUseCase.getScores()
                val result = com.agon.app.domain.model.GameResult(
                    winnerId = winnerId ?: -1,
                    winnerName = winnerId?.let { state.players.getOrNull(it)?.displayName() } ?: "تعادل",
                    scores = scores,
                    durationSeconds = 0,
                    gameMode = state.gameMode
                )
                recordGameUseCase(result, 0)
            }
        }
    }
}
