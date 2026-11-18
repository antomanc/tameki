package dev.antomanc.tameki.core.ui.format

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Test

class TransactionTitleTest {
    private val eur = CurrencyCode("EUR")
    private val occurredAt = LocalDateTime.of(2026, 11, 18, 9, 5)
    private val category = Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 1)
    private val account = Account(1, "Carta", eur, AccountType.CARD)

    private fun title(
        merchant: String? = null,
        category: Category? = null,
        account: Account? = null,
        note: String? = null,
    ) = TransactionTitle.of(
        merchant = merchant,
        category = category,
        account = account,
        note = note,
        occurredAt = occurredAt,
        locale = Locale.ITALIAN,
    )

    @Test
    fun merchantWins() {
        assertThat(title(merchant = "Esselunga", category = category, account = account, note = "Spesa"))
            .isEqualTo("Esselunga")
    }

    @Test
    fun categoryThenAccountThenNote() {
        assertThat(title(category = category, account = account, note = "Spesa")).isEqualTo("Alimentari")
        assertThat(title(account = account, note = "Spesa")).isEqualTo("Carta")
        assertThat(title(note = "Spesa")).isEqualTo("Spesa")
    }

    @Test
    fun blankMerchantDoesNotBecomeTheTitle() {
        assertThat(title(merchant = "  ", category = category)).isEqualTo("Alimentari")
    }

    @Test
    fun anonymousRowShowsItsTimeInsteadOfADash() {
        val expected = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.ITALIAN)
            .format(occurredAt)
        assertThat(title()).isEqualTo(expected)
        assertThat(title()).isNotEqualTo("—")
    }

    @Test
    fun rowOfARealTransactionIsNeverBlank() {
        val transaction = Transaction(
            id = 1,
            kind = TransactionKind.EXPENSE,
            amount = Money(1000, eur),
            accountId = 1,
            occurredAt = occurredAt,
        )
        val title = TransactionTitle.of(
            merchant = transaction.merchant,
            category = null,
            account = null,
            note = transaction.note,
            occurredAt = transaction.occurredAt,
        )
        assertThat(title).isNotEmpty()
    }
}
