package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.repository.sanitizeFts
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FilterSupportTest {
    @Test
    fun sanitizeFtsQuotesTokensAndDropsBareOperators() {
        // Quoting turns OR/AND/NOT into phrase tokens so they never act as FTS operators.
        assertThat(sanitizeFts("foo bar")).isEqualTo("\"foo\"* \"bar\"*")
        assertThat(sanitizeFts("OR AND NOT")).isEqualTo("\"OR\"* \"AND\"* \"NOT\"*")
        assertThat(sanitizeFts("latte - OR \"x\"")).isEqualTo("\"latte\"* \"OR\"* \"x\"*")
        // Tokens with no letters/digits (bare `-`) are dropped.
        assertThat(sanitizeFts("---")).isEqualTo("")
        assertThat(sanitizeFts("\"\"")).isEqualTo("")
    }
}
