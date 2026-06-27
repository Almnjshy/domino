package com.agon.app.domain.usecase.stats

import com.agon.app.domain.repository.Achievement
import com.agon.app.domain.repository.StatsRepository
import javax.inject.Inject

/**
 * Use case for getting player achievements
 */
class GetAchievementsUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): List<Achievement> {
        return statsRepository.getAchievements()
    }
}
