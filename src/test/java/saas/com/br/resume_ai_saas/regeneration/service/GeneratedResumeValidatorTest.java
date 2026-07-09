package saas.com.br.resume_ai_saas.regeneration.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;
import saas.com.br.resume_ai_saas.regeneration.exception.InvalidGeneratedResumeException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedResumeValidator")
class GeneratedResumeValidatorTest {

    private final GeneratedResumeValidator validator = new GeneratedResumeValidator();

    @Test
    @DisplayName("rejects a null payload")
    void rejectsNullPayload() {
        assertThatThrownBy(() -> validator.validateAndNormalize(null))
                .isInstanceOf(InvalidGeneratedResumeException.class);
    }

    @Test
    @DisplayName("rejects missing dados_pessoais or blank nome")
    void rejectsMissingRequiredFields() {
        GeneratedResume noPersonalData = new GeneratedResume(
                null, "resumo", List.of(), List.of(), List.of(), List.of(), List.of());
        assertThatThrownBy(() -> validator.validateAndNormalize(noPersonalData))
                .isInstanceOf(InvalidGeneratedResumeException.class);

        GeneratedResume blankName = new GeneratedResume(
                new GeneratedResume.DadosPessoais("  ", "a@b.com", null, null, null),
                "resumo", List.of(), List.of(), List.of(), List.of(), List.of());
        assertThatThrownBy(() -> validator.validateAndNormalize(blankName))
                .isInstanceOf(InvalidGeneratedResumeException.class);
    }

    @Test
    @DisplayName("rejects blank resumo_profissional")
    void rejectsBlankResumoProfissional() {
        GeneratedResume blankSummary = new GeneratedResume(
                new GeneratedResume.DadosPessoais("Maria", "a@b.com", null, null, null),
                " ", List.of(), List.of(), List.of(), List.of(), List.of());
        assertThatThrownBy(() -> validator.validateAndNormalize(blankSummary))
                .isInstanceOf(InvalidGeneratedResumeException.class);
    }

    @Test
    @DisplayName("normalizes null sections to empty lists (absent sections are never invented)")
    void normalizesNullSectionsToEmptyLists() {
        GeneratedResume input = new GeneratedResume(
                new GeneratedResume.DadosPessoais("Maria", "a@b.com", null, null, null),
                "Resumo profissional.", null, null, null, null, null);

        GeneratedResume result = validator.validateAndNormalize(input);

        assertThat(result.experiencias()).isEmpty();
        assertThat(result.educacao()).isEmpty();
        assertThat(result.skills()).isEmpty();
        assertThat(result.certificacoes()).isEmpty();
        assertThat(result.idiomas()).isEmpty();
    }

    @Test
    @DisplayName("passes a fully populated payload through unchanged")
    void passesValidPayloadThrough() {
        GeneratedResume input = new GeneratedResume(
                new GeneratedResume.DadosPessoais("Maria", "a@b.com", "11 99999-0000", "in/maria", "São Paulo"),
                "Resumo profissional.",
                List.of(new GeneratedResume.Experiencia("Dev", "Acme", "Jan 2020", null, true, List.of("Fez X"))),
                List.of(new GeneratedResume.Educacao("CC", "USP", "2016", "2020")),
                List.of("Java"),
                List.of(new GeneratedResume.Certificacao("Cert", "Org", "2021")),
                List.of(new GeneratedResume.Idioma("Inglês", "Avançado")));

        GeneratedResume result = validator.validateAndNormalize(input);

        assertThat(result).isEqualTo(input);
    }
}
