package dev.antomanc.tameki.feature.insights

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Dependencies of the Save tab: one screen, one view model, no background work. */
val insightsModule = module {
    viewModelOf(::InsightsViewModel)
}
