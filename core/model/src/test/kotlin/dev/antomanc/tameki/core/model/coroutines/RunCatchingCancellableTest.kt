package dev.antomanc.tameki.core.model.coroutines

import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertThrows
import org.junit.Test

class RunCatchingCancellableTest {
    @Test
    fun `returns the value when the block succeeds`() {
        val result = runCatchingCancellable { 42 }

        assertThat(result.getOrNull()).isEqualTo(42)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `captures an ordinary failure`() {
        val result = runCatchingCancellable { error("boom") }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `rethrows cancellation so the coroutine stays cancelled`() {
        assertThrows(CancellationException::class.java) {
            runCatchingCancellable<Unit> { throw CancellationException("cancelled") }
        }
    }
}
