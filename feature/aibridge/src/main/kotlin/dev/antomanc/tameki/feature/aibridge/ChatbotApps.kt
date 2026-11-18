package dev.antomanc.tameki.feature.aibridge

/**
 * Known chatbot packages. Chips / sheet rows are shown only when the package is installed;
 * missing apps are hidden (no web fallback URLs).
 */
object ChatbotApps {
    data class Chatbot(val id: String, val label: String, val packageName: String)

    val all: List<Chatbot> = listOf(
        Chatbot("chatgpt", "ChatGPT", "com.openai.chatgpt"),
        Chatbot("claude", "Claude", "com.anthropic.claude"),
        Chatbot("gemini", "Gemini", "com.google.android.apps.bard"),
        Chatbot("perplexity", "Perplexity", "ai.perplexity.app.android"),
        Chatbot("copilot", "Copilot", "com.microsoft.copilot"),
        Chatbot("lechat", "Le Chat", "ai.mistral.chat"),
    )

    fun installed(packageNames: Set<String>): List<Chatbot> =
        all.filter { it.packageName in packageNames }
}
