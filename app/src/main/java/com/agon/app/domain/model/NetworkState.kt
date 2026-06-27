package com.agon.app.domain.model

/**
 * Represents the network/multiplayer state
 */
data class NetworkState(
    val isConnected: Boolean = false,
    val isHost: Boolean = false,
    val roomId: String = "",
    val roomName: String = "",
    val connectedPlayers: List<NetworkPlayer> = emptyList(),
    val localPlayerId: String = "",
    val status: NetworkStatus = NetworkStatus.DISCONNECTED,
    val error: String? = null,
    val discoveredRooms: List<NetworkRoom> = emptyList()
) {
    val isOffline: Boolean get() = status == NetworkStatus.DISCONNECTED
    val canStartGame: Boolean get() = isHost && connectedPlayers.size >= 2
    val playerCount: Int get() = connectedPlayers.size
}

data class NetworkPlayer(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val pingMs: Long = 0
)

data class NetworkRoom(
    val id: String,
    val name: String,
    val hostAddress: String,
    val hostName: String,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val isPasswordProtected: Boolean = false
)

enum class NetworkStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    ERROR,
    RECONNECTING
}

sealed class NetworkEvent {
    data class PlayerJoined(val player: NetworkPlayer) : NetworkEvent()
    data class PlayerLeft(val playerId: String) : NetworkEvent()
    data class GameStateSync(val state: GameState) : NetworkEvent()
    data class PlayerAction(val action: GameAction) : NetworkEvent()
    data class Error(val message: String) : NetworkEvent()
    object ConnectionLost : NetworkEvent()
    object Reconnected : NetworkEvent()
}
