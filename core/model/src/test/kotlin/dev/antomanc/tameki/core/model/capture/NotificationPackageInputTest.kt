package dev.antomanc.tameki.core.model.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationPackageInputTest {
    @Test
    fun parsesPlainPackageName() {
        assertThat(NotificationPackageInput.parsePackageInput("com.latuabancaperandroid"))
            .isEqualTo("com.latuabancaperandroid")
    }

    @Test
    fun parsesGooglePlayUrl() {
        assertThat(
            NotificationPackageInput.parsePackageInput(
                "https://play.google.com/store/apps/details?id=com.latuabancaperandroid&hl=it",
            ),
        ).isEqualTo("com.latuabancaperandroid")
    }

    @Test
    fun parsesMarketUri() {
        assertThat(
            NotificationPackageInput.parsePackageInput("market://details?id=com.isybank.app"),
        ).isEqualTo("com.isybank.app")
    }

    @Test
    fun rejectsInvalidInput() {
        assertThat(NotificationPackageInput.parsePackageInput("")).isNull()
        assertThat(NotificationPackageInput.parsePackageInput("not a package")).isNull()
        assertThat(NotificationPackageInput.parsePackageInput("singleword")).isNull()
    }

    @Test
    fun fallbackLabelPrefersKnownBankName() {
        assertThat(NotificationPackageInput.fallbackLabel("com.latuabancaperandroid"))
            .isEqualTo("Intesa Sanpaolo")
        assertThat(NotificationPackageInput.fallbackLabel("com.example.mybank.app"))
            .isEqualTo("Mybank")
    }
}
