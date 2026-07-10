package saas.com.br.resume_ai_saas.regeneration.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test of the real LaTeX pipeline: template rendering plus
 * an actual external compiler run producing a PDF. Requires the toolchain
 * configured via the {@code LATEX_COMPILER_COMMAND} env var (or {@code tectonic}
 * on the PATH); when unavailable the test is skipped, so CI without LaTeX
 * stays green.
 */
@DisplayName("LaTeX pipeline smoke test (requires tectonic/pdflatex)")
class LatexPdfGenerationSmokeTest {

    @Test
    @DisplayName("renders and compiles a realistic resume into a valid PDF")
    void rendersAndCompilesRealPdf() throws Exception {
        String compiler = System.getenv().getOrDefault("LATEX_COMPILER_COMMAND", "tectonic");
        Assumptions.assumeTrue(compilerAvailable(compiler),
                "LaTeX compiler '" + compiler + "' not available — smoke test skipped");

        GeneratedResume resume = new GeneratedResume(
                new GeneratedResume.DadosPessoais(
                        "Maria & Silva", "maria@exemplo.com", "(11) 99999-0000",
                        "linkedin.com/in/maria_silva", "São Paulo — SP"),
                "Desenvolvedora com 100% de foco em backend Java & Spring, "
                        + "salário atual R$ 5.000, apaixonada por C# e snake_case.",
                List.of(
                        new GeneratedResume.Experiencia("Desenvolvedora Sênior", "C&A Modas",
                                "Jan 2020", null, true,
                                List.of("Aumentou a cobertura de testes em 50%",
                                        "Liderou a migração para AWS (S3, SQS & Lambda)")),
                        new GeneratedResume.Experiencia("Desenvolvedora Júnior", "Acme_Tech",
                                "03/2017", "12/2019", false,
                                List.of("Manteve APIs REST com ~99,9% de uptime"))),
                List.of(new GeneratedResume.Educacao(
                        "Ciência da Computação", "USP", "2013", "2017")),
                List.of("Java", "Spring Boot", "C#", "PostgreSQL", "LaTeX 100%"),
                List.of(new GeneratedResume.Certificacao("AWS Solutions Architect", "Amazon", "03/2023")),
                List.of(new GeneratedResume.Idioma("Inglês", "Avançado"),
                        new GeneratedResume.Idioma("Português", "Nativo")));

        String texSource = new LatexTemplateService().render(resume, false);
        byte[] pdf = new LatexCompilationService(compiler).compile(texSource);

        assertThat(pdf).hasSizeGreaterThan(1000);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(new PdfPageCounter().count(pdf)).isEqualTo(1);

        Path artifact = Path.of("target", "regeneration-smoke.pdf");
        Files.createDirectories(artifact.getParent());
        Files.write(artifact, pdf);
    }

    private static boolean compilerAvailable(String compiler) {
        try {
            Process process = new ProcessBuilder(compiler, "--version").start();
            return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
