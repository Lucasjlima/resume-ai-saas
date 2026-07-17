package saas.com.br.resume_ai_saas.regeneration.service;

import org.springframework.stereotype.Component;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;
import saas.com.br.resume_ai_saas.regeneration.exception.InvalidGeneratedResumeException;

import java.util.List;

/**
 * Sanity-checks the AI output before it reaches the LaTeX template. A failure
 * here counts as a system failure and triggers the automatic retry flow.
 */
@Component
public class GeneratedResumeValidator {

    public GeneratedResume validateAndNormalize(GeneratedResume resume) {
        if (resume == null) {
            throw new InvalidGeneratedResumeException("AI returned an empty payload");
        }
        if (resume.dadosPessoais() == null || isBlank(resume.dadosPessoais().nome())) {
            throw new InvalidGeneratedResumeException("Missing required field: dados_pessoais.nome");
        }
        if (isBlank(resume.resumoProfissional())) {
            throw new InvalidGeneratedResumeException("Missing required field: resumo_profissional");
        }
        return new GeneratedResume(
                resume.dadosPessoais(),
                resume.resumoProfissional(),
                emptyIfNull(resume.experiencias()),
                emptyIfNull(resume.educacao()),
                emptyIfNull(resume.skills()).stream()
                        .map(grupo -> new GeneratedResume.SkillGroup(
                                grupo.categoria(), emptyIfNull(grupo.itens())))
                        .toList(),
                emptyIfNull(resume.certificacoes()),
                emptyIfNull(resume.idiomas())
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
