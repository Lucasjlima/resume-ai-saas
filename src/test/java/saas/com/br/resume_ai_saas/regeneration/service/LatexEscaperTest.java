package saas.com.br.resume_ai_saas.regeneration.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LatexEscaper")
class LatexEscaperTest {

    @Test
    @DisplayName("escapes LaTeX special characters")
    void escapesSpecialCharacters() {
        assertThat(LatexEscaper.escape("C&A")).isEqualTo("C\\&A");
        assertThat(LatexEscaper.escape("100%")).isEqualTo("100\\%");
        assertThat(LatexEscaper.escape("R$ 5.000")).isEqualTo("R\\$ 5.000");
        assertThat(LatexEscaper.escape("C# e F#")).isEqualTo("C\\# e F\\#");
        assertThat(LatexEscaper.escape("snake_case")).isEqualTo("snake\\_case");
        assertThat(LatexEscaper.escape("{java}")).isEqualTo("\\{java\\}");
        assertThat(LatexEscaper.escape("a~b")).isEqualTo("a\\textasciitilde{}b");
        assertThat(LatexEscaper.escape("a^b")).isEqualTo("a\\textasciicircum{}b");
        assertThat(LatexEscaper.escape("a\\b")).isEqualTo("a\\textbackslash{}b");
    }

    @Test
    @DisplayName("returns empty string for null input")
    void nullBecomesEmpty() {
        assertThat(LatexEscaper.escape(null)).isEmpty();
    }

    @Test
    @DisplayName("leaves plain text untouched (including accents)")
    void plainTextUntouched() {
        assertThat(LatexEscaper.escape("Desenvolvedora Sênior em São Paulo"))
                .isEqualTo("Desenvolvedora Sênior em São Paulo");
    }

    @Test
    @DisplayName("converts unicode dashes to LaTeX ligatures (glyphs missing in Computer Modern)")
    void convertsUnicodeDashes() {
        assertThat(LatexEscaper.escape("São Paulo — SP")).isEqualTo("São Paulo --- SP");
        assertThat(LatexEscaper.escape("2019–2021")).isEqualTo("2019--2021");
    }
}
