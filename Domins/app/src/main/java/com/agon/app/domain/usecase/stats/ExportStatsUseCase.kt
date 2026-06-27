package com.agon.app.domain.usecase.stats

import com.agon.app.domain.repository.StatsRepository
import javax.inject.Inject

/**
 * Use case for exporting statistics to JSON
 */
class ExportStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Result<String> {
        return statsRepository.exportToJson()
    }
}
