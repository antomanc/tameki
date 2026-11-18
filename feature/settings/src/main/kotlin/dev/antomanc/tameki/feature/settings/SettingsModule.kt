package dev.antomanc.tameki.feature.settings

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel {
        val config = get<SettingsConfig>()
        SettingsViewModel(
            settings = get(),
            settingsImpl = get(),
            accounts = get(),
            categories = get(),
            transactions = get(),
            backup = get(),
            csv = get(),
            exchangeRates = get(),
            captureBridge = get(),
            nanoBridge = get(),
            clock = get(),
            versionName = config.versionName,
            isDebug = config.isDebug,
        )
    }
}
