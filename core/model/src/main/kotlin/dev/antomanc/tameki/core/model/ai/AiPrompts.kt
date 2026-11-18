package dev.antomanc.tameki.core.model.ai

import java.util.Locale

/**
 * Ready-to-copy prompts for external chatbots.
 * Locale picks IT vs EN wording; category/account names are embedded so the model maps hints correctly.
 */
object AiPrompts {
    fun receiptExtraction(
        locale: Locale,
        categoryNames: List<String>,
        accountNames: List<String>,
    ): String {
        val lang = if (locale.language == "it") "it" else "en"
        val cats = categoryNames.joinToString(", ")
        val accounts = accountNames.joinToString(", ")
        return if (lang == "it") {
            """
            Sei un assistente per Tameki, un'app di spese. Estrai le transazioni da questo scontrino.
            Rispondi SOLO con un oggetto JSON valido secondo lo schema tameki v1 (campo "tameki": 1).
            Categorie disponibili: $cats
            Conti disponibili: $accounts
            Usa amount come stringa con punto decimale (es. "12.50"), currency ISO, date ISO-8601.
            Se un campo non è chiaro, omettilo invece di inventarlo.
            """.trimIndent()
        } else {
            """
            You are helping Tameki, an expense app. Extract transactions from this receipt.
            Reply with ONLY a valid JSON object matching the tameki v1 schema ("tameki": 1).
            Available categories: $cats
            Available accounts: $accounts
            Use amount as a string with a dot decimal (e.g. "12.50"), ISO currency, ISO-8601 dates.
            If a field is unclear, omit it rather than inventing values.
            """.trimIndent()
        }
    }

    fun naturalLanguageEntry(
        locale: Locale,
        categoryNames: List<String>,
        accountNames: List<String>,
    ): String {
        val lang = if (locale.language == "it") "it" else "en"
        val cats = categoryNames.joinToString(", ")
        val accounts = accountNames.joinToString(", ")
        return if (lang == "it") {
            """
            Converti il testo dell'utente in JSON tameki v1 (campo "tameki": 1) con un array "transactions".
            Categorie: $cats
            Conti: $accounts
            Non aggiungere spiegazioni fuori dal JSON.
            """.trimIndent()
        } else {
            """
            Convert the user's text into tameki v1 JSON ("tameki": 1) with a "transactions" array.
            Categories: $cats
            Accounts: $accounts
            Do not add any explanation outside the JSON.
            """.trimIndent()
        }
    }

    fun subscriptionDiscovery(
        locale: Locale,
        categoryNames: List<String>,
    ): String {
        val cats = categoryNames.joinToString(", ")
        return if (locale.language == "it") {
            """
            Dal movimento del conto individua abbonamenti e ricorrenze.
            Rispondi con JSON tameki v1: "tameki": 1 e array "subscriptions"
            (name, amount, currency, cycle tipo "1 month", next_date, category).
            Categorie: $cats
            Solo JSON, senza prose.
            """.trimIndent()
        } else {
            """
            From this bank statement, identify subscriptions and recurring charges.
            Reply with tameki v1 JSON: "tameki": 1 and a "subscriptions" array
            (name, amount, currency, cycle like "1 month", next_date, category).
            Categories: $cats
            JSON only, no prose.
            """.trimIndent()
        }
    }

    fun monthReview(locale: Locale, summary: MonthSummary): String {
        val body = summary.toMarkdown()
        return if (locale.language == "it") {
            """
            Sei un consulente di risparmio. Analizza questo riepilogo mensile di Tameki e suggerisci
            azioni concrete (disdette, passaggi ad annuale, tagli di categoria) citando solo i numeri presenti.
            Non inventare prezzi annuali se non sono nel riepilogo.

            $body
            """.trimIndent()
        } else {
            """
            You are a savings coach. Review this Tameki monthly summary and suggest concrete actions
            (cancels, switch to annual, category cuts) using only the numbers shown.
            Do not invent annual prices that are not in the summary.

            $body
            """.trimIndent()
        }
    }
}
