package dev.antomanc.tameki.feature.activity

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val activityModule = module {
    viewModelOf(::ActivityViewModel)
    viewModel {
        QuickAddViewModel(
            transactions = get(),
            accounts = get(),
            categories = get(),
            receipts = get(),
            inbox = get(),
            settings = get(),
            receiptReader = get(),
            clock = get(),
            appContext = androidContext(),
        )
    }
}
