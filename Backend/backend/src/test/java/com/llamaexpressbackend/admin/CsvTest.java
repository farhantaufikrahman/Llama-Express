package com.llamaexpressbackend.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTest {

    @Test
    void quotesValuesWithCommaQuoteOrNewline() {
        assertThat(Csv.text("Jl. Dago, No. 1")).isEqualTo("\"Jl. Dago, No. 1\"");
        assertThat(Csv.text("Kata \"kunci\"")).isEqualTo("\"Kata \"\"kunci\"\"\"");
        assertThat(Csv.text("baris1\nbaris2")).isEqualTo("\"baris1\nbaris2\"");
    }

    @Test
    void neutralizesFormulaInjection() {
        assertThat(Csv.text("=HYPERLINK(\"http://jahat\")")).startsWith("\"'=HYPERLINK");
        assertThat(Csv.text("+62812")).isEqualTo("'+62812");
        assertThat(Csv.text("@SUM(A1)")).isEqualTo("'@SUM(A1)");
    }

    @Test
    void rawValues_areNotPrefixed() {
        assertThat(Csv.raw(-6.9)).isEqualTo("-6.9"); // angka negatif buatan sistem tetap utuh
        assertThat(Csv.raw(null)).isEmpty();
    }
}