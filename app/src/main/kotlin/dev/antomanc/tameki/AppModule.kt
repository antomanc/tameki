package dev.antomanc.tameki

import dev.antomanc.tameki.feature.settings.CaptureSettingsBridge
import dev.antomanc.tameki.feature.settings.GeminiNanoBridge
import dev.antomanc.tameki.feature.settings.SettingsConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        SettingsConfig(
            versionName = BuildConfig.VERSION_NAME,
            isDebug = BuildConfig.DEBUG,
        )
    }
    single<CaptureSettingsBridge> { CaptureSettingsBridgeImpl(androidContext()) }
    single<GeminiNanoBridge> { GeminiNanoBridgeImpl(get()) }
}
