package com.agon.app.data.repository

import com.agon.app.domain.model.GameAction
import com.agon.app.domain.model.GameState
import com.agon.app.domain.model.NetworkEvent
import com.agon.app.domain.model.NetworkPlayer
import com.agon.app.domain.model.NetworkRoom
import com.agon.app.domain.model.NetworkState
import com.agon.app.domain.model.NetworkStatus
import com.agon.app.domain.repository.NetworkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NetworkRepository
 * Handles WiFi Direct / Nearby Connections for multiplayer
 */
@Singleton
class NetworkRepositoryImpl @Inject constructor() : NetworkRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _networkState = MutableStateFlow(NetworkState())
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _events = MutableSharedFlow<NetworkEvent>(extraBufferCapacity = 64)
    override val events: Flow<NetworkEvent> = _events.asSharedFlow()

    private var discoveryJob: kotlinx.coroutines.Job? = null
    private var heartbeatJob: kotlinx.coroutines.Job? = null

    override suspend fun createRoom(roomName: String, maxPlayers: Int): Result<NetworkState> {
        return try {
            val roomId = UUID.randomUUID().toString()
            val localPlayer = NetworkPlayer(
                id = "host_${System.currentTimeMillis()}",
                name = "Host",
                isHost = true,
                isReady = true
            )

            val newState = NetworkState(
                isConnected = true,
                isHost = true,
                roomId = roomId,
                roomName = roomName,
                connectedPlayers = listOf(localPlayer),
                localPlayerId = localPlayer.id,
                status = NetworkStatus.CONNECTED
            )

            _networkState.value = newState
            startHeartbeat()

            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun discoverRooms(): Result<List<NetworkRoom>> {
        return try {
            _networkState.value = _networkState.value.copy(status = NetworkStatus.SYNCING)

            // Simulate discovery (replace with actual WiFi Direct / Nearby API)
            delay(1500)

            val mockRooms = listOf(
                NetworkRoom(
                    id = "room_1",
                    name = "غرفة اللاعب 1",
                    hostAddress = "192.168.1.100",
                    hostName = "Player1",
                    currentPlayers = 2,
                    maxPlayers = 4
                ),
                NetworkRoom(
                    id = "room_2",
                    name = "غرفة اللاعب 2",
                    hostAddress = "192.168.1.101",
                    hostName = "Player2",
                    currentPlayers = 1,
                    maxPlayers = 4
                )
            )

            _networkState.value = _networkState.value.copy(
                discoveredRooms = mockRooms,
                status = NetworkStatus.DISCONNECTED
            )

            Result.success(mockRooms)
        } catch (e: Exception) {
            _networkState.value = _networkState.value.copy(status = NetworkStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    override suspend fun joinRoom(room: NetworkRoom, playerName: String): Result<NetworkState> {
        return try {
            _networkState.value = _networkState.value.copy(status = NetworkStatus.CONNECTING)

            delay(1000) // Simulate connection

            val localPlayer = NetworkPlayer(
                id = "client_${System.currentTimeMillis()}",
                name = playerName,
                isHost = false
            )

            val newState = NetworkState(
                isConnected = true,
                isHost = false,
                roomId = room.id,
                roomName = room.name,
                connectedPlayers = listOf(localPlayer),
                localPlayerId = localPlayer.id,
                status = NetworkStatus.CONNECTED
            )

            _networkState.value = newState
            _events.emit(NetworkEvent.PlayerJoined(localPlayer))
            startHeartbeat()

            Result.success(newState)
        } catch (e: Exception) {
            _networkState.value = _networkState.value.copy(status = NetworkStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    override suspend fun leaveRoom(): Result<Unit> {
        return try {
            stopHeartbeat()
            _networkState.value = NetworkState()
            _events.emit(NetworkEvent.ConnectionLost)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendGameAction(action: GameAction): Result<Unit> {
        return try {
            _events.emit(NetworkEvent.PlayerAction(action))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncGameState(state: GameState): Result<Unit> {
        return try {
            _events.emit(NetworkEvent.GameStateSync(state))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startGame(): Result<Unit> {
        return try {
            if (!_networkState.value.isHost) {
                return Result.failure(IllegalStateException("Only host can start game"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return leaveRoom()
    }

    override suspend fun reconnect(): Result<NetworkState> {
        return try {
            _networkState.value = _networkState.value.copy(status = NetworkStatus.RECONNECTING)
            delay(2000)

            // Attempt reconnection logic here
            _networkState.value = _networkState.value.copy(status = NetworkStatus.CONNECTED)
            _events.emit(NetworkEvent.Reconnected)

            Result.success(_networkState.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setReady(isReady: Boolean): Result<Unit> {
        return try {
            val currentState = _networkState.value
            val updatedPlayers = currentState.connectedPlayers.map {
                if (it.id == currentState.localPlayerId) it.copy(isReady = isReady) else it
            }
            _networkState.value = currentState.copy(connectedPlayers = updatedPlayers)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(5000)
                // Send heartbeat to keep connection alive
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
