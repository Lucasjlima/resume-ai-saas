package saas.com.br.resume_ai_saas.regeneration.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import saas.com.br.resume_ai_saas.regeneration.exception.LatexCompilationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LatexCompilationService")
class LatexCompilationServiceTest {

    @Test
    @DisplayName("wraps a missing compiler binary in LatexCompilationException")
    void missingCompilerBinaryThrows() {
        LatexCompilationService service =
                new LatexCompilationService("definitely-not-a-latex-compiler-xyz");

        assertThatThrownBy(() -> service.compile("\\documentclass{article}\\begin{document}x\\end{document}"))
                .isInstanceOf(LatexCompilationException.class);
    }
}
