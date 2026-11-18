package dev.antomanc.tameki.feature.aibridge

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val aibridgeModule = module {
    viewModel {
        AiBridgeViewModel(
            transactions = get(),
            recurrings = get(),
            accounts = get(),
            categories = get(),
            settings = get(),
            clock = get(),
            appContext = androidContext(),
        )
    }
}
