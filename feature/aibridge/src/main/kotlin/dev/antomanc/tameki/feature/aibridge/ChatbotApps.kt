package dev.antomanc.tameki.feature.aibridge

/**
 * Known chatbot packages. Chips are shown only when the package is installed;
 * missing apps are hidden (no web fallback URLs).
 */
object ChatbotApps {
    data class Chatbot(val id: String, val label: String, val packageName: String)

    val all: List<Chatbot> = listOf(
        Chatbot("chatgpt", "ChatGPT", "com.openai.chatgpt"),
        Chatbot("claude", "Claude", "com.anthropic.claude"),
        Chatbot("gemini", "Gemini", "com.google.android.apps.bard"),
    )

    fun installed(packageNames: Set<String>): List<Chatbot> =
        all.filter { it.packageName in packageNames }
}
