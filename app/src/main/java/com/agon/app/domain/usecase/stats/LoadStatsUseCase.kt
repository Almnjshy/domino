package com.agon.app.domain.usecase.stats

import com.agon.app.domain.model.StatsData
import com.agon.app.domain.repository.StatsRepository
import javax.inject.Inject

/**
 * Use case for loading player statistics
 */
class LoadStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): StatsData {
        return statsRepository.loadStats()
    }
}
