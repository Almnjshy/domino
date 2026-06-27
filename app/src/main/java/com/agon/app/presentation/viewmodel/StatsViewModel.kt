package com.agon.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.domain.model.StatsData
import com.agon.app.domain.repository.Achievement
import com.agon.app.domain.usecase.stats.ClearStatsUseCase
import com.agon.app.domain.usecase.stats.ExportStatsUseCase
import com.agon.app.domain.usecase.stats.GetAchievementsUseCase
import com.agon.app.domain.usecase.stats.LoadStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Stats screen with Hilt
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val loadStatsUseCase: LoadStatsUseCase,
    private val clearStatsUseCase: ClearStatsUseCase,
    private val exportStatsUseCase: ExportStatsUseCase,
    private val getAchievementsUseCase: GetAchievementsUseCase
) : ViewModel() {

    data class StatsUiState(
        val stats: StatsData = StatsData(),
        val achievements: List<Achievement> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val showClearConfirmation: Boolean = false,
        val exportedJson: String? = null,
        val showExportDialog: Boolean = false
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        loadAchievements()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val stats = loadStatsUseCase()
                _uiState.value = _uiState.value.copy(
                    stats = stats,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadAchievements() {
        viewModelScope.launch {
            try {
                val achievements = getAchievementsUseCase()
                _uiState.value = _uiState.value.copy(achievements = achievements)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            clearStatsUseCase()
                .onSuccess {
                    loadStats()
                    loadAchievements()
                    _uiState.value = _uiState.value.copy(
                        showClearConfirmation = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    fun exportStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            exportStatsUseCase()
                .onSuccess { json ->
                    _uiState.value = _uiState.value.copy(
                        exportedJson = json,
                        isLoading = false,
                        showExportDialog = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    fun showClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = true)
    }

    fun dismissClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = false)
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false, exportedJson = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
