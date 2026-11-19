package dev.antomanc.tameki.core.model.coroutines

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but a cancelled coroutine stays cancelled instead of turning into a failure.
 *
 * Use this for any work that can suspend: plain `runCatching` catches [CancellationException], so
 * a cancelled screen or worker looks like an ordinary error and keeps running after cancellation.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }
