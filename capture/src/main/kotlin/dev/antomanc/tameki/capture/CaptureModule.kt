package dev.antomanc.tameki.capture

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Opt-in notification capture: reads notifications from allowlisted banking and wallet apps on
 * device and offers drafts in the Inbox. Nothing leaves the phone.
 */
val captureModule = module {
    single { NotificationDeduper() }
    single {
        InboxAccept(
            inbox = get(),
            transactions = get(),
            categories = get(),
            accounts = get(),
            clock = get(),
        )
    }
    single {
        InboxNotifier(
            context = androidContext(),
            inbox = get(),
        )
    }
    single {
        val notifier: InboxNotifier = get()
        PaymentNotificationProcessor(
            inbox = get(),
            clock = get(),
            deduper = get(),
            onInboxChanged = { notifier.refresh() },
        )
    }
}
