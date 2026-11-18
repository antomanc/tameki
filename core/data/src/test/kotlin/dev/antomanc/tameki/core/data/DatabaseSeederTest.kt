package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DatabaseSeederTest : DbTestRule() {
    @Test
    fun seedIsAtomicAccountsAndCategoriesTogether() = runTest {
        seed()
        val cats = categories.observeAll(includeArchived = true).first()
        val accs = accounts.observeAll(includeArchived = true).first()
        assertThat(cats).isNotEmpty()
        assertThat(accs).hasSize(2)
        // Second call is a no-op.
        seed()
        assertThat(categories.observeAll(includeArchived = true).first()).hasSize(cats.size)
        assertThat(accounts.observeAll(includeArchived = true).first()).hasSize(2)
    }
}
