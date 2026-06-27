package com.agon.app.domain.usecase.stats

import com.agon.app.domain.repository.StatsRepository
import javax.inject.Inject

/**
 * Use case for clearing all statistics
 */
class ClearStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return statsRepository.clearStats()
    }
}
