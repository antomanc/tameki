package dev.antomanc.tameki.feature.subscriptions

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Dependencies of the Recurring tab: the bundled brand catalogue and its four screens. */
val subscriptionsModule = module {
    single { BrandCatalog.load(androidContext()) }
    viewModelOf(::SubscriptionsViewModel)
    viewModelOf(::RecurringDetailViewModel)
    viewModelOf(::RecurringEditViewModel)
    viewModelOf(::SubscriptionsReviewViewModel)
}
