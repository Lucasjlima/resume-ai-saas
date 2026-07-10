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

    @Test
    @DisplayName("compact layout fits a long resume in one page where the regular layout overflows")
    void compactLayoutFitsLongResumeInOnePage() {
        String compiler = System.getenv().getOrDefault("LATEX_COMPILER_COMMAND", "tectonic");
        Assumptions.assumeTrue(compilerAvailable(compiler),
                "LaTeX compiler '" + compiler + "' not available — smoke test skipped");

        GeneratedResume resume = longResume();
        LatexTemplateService templates = new LatexTemplateService();
        LatexCompilationService compilation = new LatexCompilationService(compiler);
        PdfPageCounter counter = new PdfPageCounter();

        int regularPages = counter.count(compilation.compile(templates.render(resume, false)));
        int compactPages = counter.count(compilation.compile(templates.render(resume, true)));

        assertThat(regularPages).isGreaterThan(1);
        assertThat(compactPages).isEqualTo(1);
    }

    private static GeneratedResume longResume() {
        List<String> bullets = List.of(
                "Projetou e manteve microsserviços Spring Boot atendendo milhões de requisições mensais, "
                        + "com foco em resiliência, idempotência e tolerância a falhas em cenários de pico",
                "Reduziu o tempo de resposta médio das APIs em 40% com cache distribuído em Redis, "
                        + "profiling de queries e otimização de índices no PostgreSQL",
                "Implantou observabilidade completa com métricas, tracing distribuído e alertas, "
                        + "reduzindo o tempo médio de detecção de incidentes de horas para minutos",
                "Mentorou desenvolvedores júnior em boas práticas de testes automatizados, "
                        + "code review e desenho de APIs, elevando a cobertura de testes do time");
        List<GeneratedResume.Experiencia> experiencias = List.of(
                new GeneratedResume.Experiencia("Engenheira de Software Sênior", "Empresa Alpha",
                        "Jan 2022", null, true, bullets),
                new GeneratedResume.Experiencia("Engenheira de Software Plena", "Empresa Beta",
                        "Mar 2019", "Dez 2021", false, bullets),
                new GeneratedResume.Experiencia("Desenvolvedora Backend", "Empresa Gama",
                        "Jun 2017", "Fev 2019", false, bullets),
                new GeneratedResume.Experiencia("Desenvolvedora Júnior", "Empresa Épsilon",
                        "Jun 2016", "Mai 2017", false, bullets),
                new GeneratedResume.Experiencia("Estagiária de Desenvolvimento", "Empresa Delta",
                        "Jan 2015", "Mai 2016", false, bullets));
        return new GeneratedResume(
                new GeneratedResume.DadosPessoais(
                        "Maria da Silva Santos", "maria.santos@exemplo.com", "(11) 99999-0000",
                        "linkedin.com/in/maria-santos", "São Paulo — SP"),
                "Engenheira de software com mais de 8 anos de experiência em backend Java, "
                        + "arquitetura de microsserviços e liderança técnica de squads. "
                        + "Focada em sistemas escaláveis, observabilidade e cultura de qualidade.",
                experiencias,
                List.of(new GeneratedResume.Educacao("Ciência da Computação", "USP", "2012", "2016"),
                        new GeneratedResume.Educacao("MBA em Arquitetura de Software", "FIA", "2018", "2020")),
                List.of("Java", "Spring Boot", "PostgreSQL", "AWS", "Docker", "Kubernetes",
                        "Kafka", "Redis", "Terraform", "Observabilidade"),
                List.of(new GeneratedResume.Certificacao("AWS Solutions Architect", "Amazon", "03/2023"),
                        new GeneratedResume.Certificacao("CKAD", "CNCF", "08/2022")),
                List.of(new GeneratedResume.Idioma("Inglês", "Avançado"),
                        new GeneratedResume.Idioma("Espanhol", "Intermediário"),
                        new GeneratedResume.Idioma("Português", "Nativo")));
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
