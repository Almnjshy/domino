package com.agon.app.domain.usecase.game

import com.agon.app.domain.model.GameMode
import com.agon.app.domain.model.GameState
import com.agon.app.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case for starting a new game
 */
class NewGameUseCase @Inject constructor(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(mode: GameMode): Result<GameState> {
        return gameRepository.newGame(mode)
    }
}
