package dev.antomanc.tameki.automation

/**
 * Silent automation saves require both the user setting and the caller's save flag.
 */
object SilentSavePolicy {
    fun shouldSilentSave(settingEnabled: Boolean, saveRequested: Boolean): Boolean =
        settingEnabled && saveRequested
}
