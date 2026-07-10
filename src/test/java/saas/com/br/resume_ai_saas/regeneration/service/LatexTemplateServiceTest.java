package saas.com.br.resume_ai_saas.regeneration.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LatexTemplateService")
class LatexTemplateServiceTest {

    private final LatexTemplateService service = new LatexTemplateService();

    @Test
    @DisplayName("renders a complete resume with escaped values and free-form dates")
    void rendersCompleteResume() {
        GeneratedResume resume = new GeneratedResume(
                new GeneratedResume.DadosPessoais(
                        "Maria & Silva", "maria@ex.com", "11 99999-0000", "linkedin.com/in/maria_s", "São Paulo"),
                "Resumo com 100% de foco.",
                List.of(new GeneratedResume.Experiencia(
                        "Dev C#", "C&A", "Jan 2020", null, true, List.of("Aumentou 50% a cobertura"))),
                List.of(new GeneratedResume.Educacao("Ciência da Computação", "USP", "2016", "2020")),
                List.of("Java", "C#"),
                List.of(new GeneratedResume.Certificacao("AWS SAA", "Amazon", "03/2023")),
                List.of(new GeneratedResume.Idioma("Inglês", "Avançado")));

        String tex = service.render(resume, false);

        assertThat(tex).contains("\\documentclass");
        assertThat(tex).contains("11pt");
        assertThat(tex).contains("margin=2.2cm");
        assertThat(tex).contains("Maria \\& Silva");
        assertThat(tex).contains("Resumo com 100\\% de foco.");
        assertThat(tex).contains("C\\&A");
        assertThat(tex).contains("Dev C\\#");
        assertThat(tex).contains("Jan 2020");
        assertThat(tex).contains("Atual");
        assertThat(tex).contains("linkedin.com/in/maria\\_s");
        assertThat(tex).contains("AWS SAA");
        assertThat(tex).contains("Inglês");
    }

    @Test
    @DisplayName("omits sections that came back empty")
    void omitsEmptySections() {
        GeneratedResume resume = new GeneratedResume(
                new GeneratedResume.DadosPessoais("Maria", "maria@ex.com", null, null, null),
                "Resumo.",
                List.of(),
                List.of(),
                List.of("Java"),
                List.of(),
                List.of());

        String tex = service.render(resume, false);

        assertThat(tex).doesNotContain("Experiência Profissional");
        assertThat(tex).doesNotContain("Formação Acadêmica");
        assertThat(tex).doesNotContain("Certificações");
        assertThat(tex).doesNotContain("Idiomas");
        assertThat(tex).contains("Habilidades");
    }

    @Test
    @DisplayName("compact mode shrinks font, margins and spacing for the one-page fallback")
    void compactModeUsesDenserLayout() {
        GeneratedResume resume = new GeneratedResume(
                new GeneratedResume.DadosPessoais("Maria", "maria@ex.com", null, null, null),
                "Resumo.",
                List.of(), List.of(), List.of("Java"), List.of(), List.of());

        String tex = service.render(resume, true);

        assertThat(tex).contains("10pt");
        assertThat(tex).contains("margin=1.5cm");
        assertThat(tex).doesNotContain("11pt");
        assertThat(tex).doesNotContain("margin=2.2cm");
    }
}
