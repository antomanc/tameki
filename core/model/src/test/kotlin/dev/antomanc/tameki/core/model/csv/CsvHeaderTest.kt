package dev.antomanc.tameki.core.model.csv

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvHeaderTest {
    @Test fun commaHeaders() {
        assertThat(CsvHeader.parse("date,amount,merchant")).containsExactly("date", "amount", "merchant").inOrder()
    }

    @Test fun semicolonHeadersWithABom() {
        assertThat(CsvHeader.parse("\uFEFFData;Importo;Descrizione"))
            .containsExactly("Data", "Importo", "Descrizione").inOrder()
    }

    @Test fun quotedNamesKeepTheirDelimiters() {
        assertThat(CsvHeader.parse("\"Data, ora\";Importo;\"Note \"\"extra\"\"\""))
            .containsExactly("Data, ora", "Importo", "Note \"extra\"").inOrder()
    }

    @Test fun tabHeaders() {
        assertThat(CsvHeader.parse("a\tb\tc")).containsExactly("a", "b", "c").inOrder()
    }

    @Test fun blankLineHasNoHeaders() {
        assertThat(CsvHeader.parse("")).isEmpty()
    }
}
