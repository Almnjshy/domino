package com.agon.app.di

import android.content.Context
import com.agon.app.data.repository.GameRepositoryImpl
import com.agon.app.data.repository.NetworkRepositoryImpl
import com.agon.app.data.repository.SettingsRepositoryImpl
import com.agon.app.data.repository.StatsRepositoryImpl
import com.agon.app.domain.engine.ai.DominoAIEngine
import com.agon.app.domain.engine.game.DominoGameEngine
import com.agon.app.domain.repository.GameRepository
import com.agon.app.domain.repository.NetworkRepository
import com.agon.app.domain.repository.SettingsRepository
import com.agon.app.domain.repository.StatsRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Main application module with Engine layer
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    // Game Engine (pure Kotlin, no Android deps)
    @Provides
    @Singleton
    fun provideDominoGameEngine(): DominoGameEngine = DominoGameEngine()

    // AI Engine (pure Kotlin, no Android deps)
    @Provides
    @Singleton
    fun provideDominoAIEngine(): DominoAIEngine = DominoAIEngine()

    // Repositories with Engine injection
    @Provides
    @Singleton
    fun provideGameRepository(
        gameEngine: DominoGameEngine,
        aiEngine: DominoAIEngine
    ): GameRepository = GameRepositoryImpl(gameEngine, aiEngine)

    @Provides
    @Singleton
    fun provideNetworkRepository(): NetworkRepository = NetworkRepositoryImpl()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideStatsRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): StatsRepository = StatsRepositoryImpl(context, gson)
}
