package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["seedKey"], unique = false)],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val currency: String,
    val type: String,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val seedKey: String? = null,
    val customName: String? = null,
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        currency = CurrencyCode(currency),
        type = AccountType.valueOf(type),
        archived = archived,
        sortOrder = sortOrder,
        seedKey = seedKey,
        customName = customName,
    )

    companion object {
        fun from(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            currency = account.currency.value,
            type = account.type.name,
            archived = account.archived,
            sortOrder = account.sortOrder,
            seedKey = account.seedKey,
            customName = account.customName,
        )
    }
}
