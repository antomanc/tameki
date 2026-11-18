package dev.antomanc.tameki.feature.aibridge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatbotAppsTest {
    @Test
    fun hidesMissingPackages() {
        val installed = ChatbotApps.installed(setOf("com.openai.chatgpt"))
        assertThat(installed).hasSize(1)
        assertThat(installed.first().label).isEqualTo("ChatGPT")
    }
}
