package dev.antomanc.tameki.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.antomanc.tameki.core.model.entity.DefaultAccounts
import dev.antomanc.tameki.core.model.entity.DefaultCategories

/**
 * v1 → v2: seedKey + customName on categories and accounts so locale changes do not freeze names.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN seedKey TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN customName TEXT")
        db.execSQL("ALTER TABLE accounts ADD COLUMN seedKey TEXT")
        db.execSQL("ALTER TABLE accounts ADD COLUMN customName TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_seedKey ON categories(seedKey)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_seedKey ON accounts(seedKey)")

        db.query("SELECT id, name FROM categories").use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                val key = DefaultCategories.seedKeyForName(name)
                if (key != null) {
                    db.execSQL(
                        "UPDATE categories SET seedKey = ?, customName = NULL WHERE id = ?",
                        arrayOf<Any?>(key, id),
                    )
                } else {
                    db.execSQL(
                        "UPDATE categories SET customName = ? WHERE id = ?",
                        arrayOf<Any?>(name, id),
                    )
                }
            }
        }

        db.query("SELECT id, name FROM accounts").use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                val key = DefaultAccounts.seedKeyForName(name)
                if (key != null) {
                    db.execSQL(
                        "UPDATE accounts SET seedKey = ?, customName = NULL WHERE id = ?",
                        arrayOf<Any?>(key, id),
                    )
                } else {
                    db.execSQL(
                        "UPDATE accounts SET customName = ? WHERE id = ?",
                        arrayOf<Any?>(name, id),
                    )
                }
            }
        }
    }
}
