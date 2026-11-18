package dev.antomanc.tameki.capture

import org.koin.dsl.module

/**
 * Opt-in notification capture: reads notifications from allowlisted banking and wallet apps on
 * device and offers drafts in the Inbox. Receipt photos (CameraX) are captured here too.
 * Nothing leaves the phone.
 */
val captureModule = module {
}
