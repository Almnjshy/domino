package com.agon.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.domain.model.NetworkRoom
import com.agon.app.domain.model.NetworkState
import com.agon.app.domain.usecase.network.CreateRoomUseCase
import com.agon.app.domain.usecase.network.DiscoverRoomsUseCase
import com.agon.app.domain.usecase.network.JoinRoomUseCase
import com.agon.app.domain.usecase.network.LeaveRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Network/Multiplayer screen with Hilt
 */
@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val createRoomUseCase: CreateRoomUseCase,
    private val discoverRoomsUseCase: DiscoverRoomsUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase
) : ViewModel() {

    data class NetworkUiState(
        val networkState: NetworkState = NetworkState(),
        val discoveredRooms: List<NetworkRoom> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val showCreateDialog: Boolean = false
    )

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    fun createRoom(roomName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            createRoomUseCase(roomName)
                .onSuccess { state ->
                    _uiState.value = _uiState.value.copy(
                        networkState = state,
                        isLoading = false,
                        showCreateDialog = false
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

    fun discoverRooms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            discoverRoomsUseCase()
                .onSuccess { rooms ->
                    _uiState.value = _uiState.value.copy(
                        discoveredRooms = rooms,
                        isLoading = false
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

    fun joinRoom(room: NetworkRoom, playerName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            joinRoomUseCase(room, playerName)
                .onSuccess { state ->
                    _uiState.value = _uiState.value.copy(
                        networkState = state,
                        isLoading = false
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

    fun leaveRoom() {
        viewModelScope.launch {
            leaveRoomUseCase()
            _uiState.value = _uiState.value.copy(networkState = NetworkState())
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
