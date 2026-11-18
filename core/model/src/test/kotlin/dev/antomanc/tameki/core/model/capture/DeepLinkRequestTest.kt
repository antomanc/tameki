package dev.antomanc.tameki.core.model.capture

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.TransactionKind
import org.junit.Test

class DeepLinkRequestTest {
    @Test
    fun parsesFullLink() {
        val result = DeepLinkRequest.parse(
            "tameki://add?amount=12.50&currency=EUR&kind=expense&merchant=Esselunga&category=Groceries&account=Card&date=2026-11-18&note=Latte&tags=food,shop&save=0",
        ).getOrThrow()
        assertThat(result.save).isFalse()
        assertThat(result.draft.amount?.minor).isEqualTo(1250L)
        assertThat(result.draft.merchant).isEqualTo("Esselunga")
        assertThat(result.draft.kind).isEqualTo(TransactionKind.EXPENSE)
        assertThat(result.tags).containsExactly("food", "shop")
    }

    @Test
    fun silentSaveFlag() {
        val result = DeepLinkRequest.parse("tameki://add?amount=1&save=1").getOrThrow()
        assertThat(result.save).isTrue()
    }

    @Test
    fun rejectsBadScheme() {
        val result = DeepLinkRequest.parse("https://add?amount=1")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun rejectsBadAmount() {
        val result = DeepLinkRequest.parse("tameki://add?amount=nope&currency=EUR")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun defaultsCurrency() {
        val result = DeepLinkRequest.parse(
            "tameki://add?amount=3",
            defaultCurrency = CurrencyCode("USD"),
        ).getOrThrow()
        assertThat(result.draft.amount?.currency).isEqualTo(CurrencyCode("USD"))
    }
}
