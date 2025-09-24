package com.example.pixvi.repo

import com.example.pixvi.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * A repository that acts as the single source of truth for all battery saver related theme
 */
class BatterySaverThemeRepository(
    settingsRepository: SettingsRepository,
    systemInfoRepository: SystemInfoRepository,
    externalScope: CoroutineScope
) {
    val batterSaver: StateFlow<Boolean> = combine(
        settingsRepository.isBatterySaver,
        systemInfoRepository.isBatterySaverOn()
    ) { userWantsIt, systemNeedsIt ->

        systemNeedsIt || userWantsIt

    }.stateIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false
    )
}