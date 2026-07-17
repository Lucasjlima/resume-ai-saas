package saas.com.br.resume_ai_saas.regeneration.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * First structured representation of the resume content (the original resume
 * is stored as flat text). Produced by the AI from the original text plus the
 * analysis feedback; consumed by the FreeMarker LaTeX template. Field names
 * follow the JSON contract agreed in the spec (Portuguese, snake_case).
 */
public record GeneratedResume(
        @JsonProperty("dados_pessoais") DadosPessoais dadosPessoais,
        @JsonProperty("resumo_profissional") String resumoProfissional,
        @JsonProperty("experiencias") List<Experiencia> experiencias,
        @JsonProperty("educacao") List<Educacao> educacao,
        @JsonProperty("skills") List<SkillGroup> skills,
        @JsonProperty("certificacoes") List<Certificacao> certificacoes,
        @JsonProperty("idiomas") List<Idioma> idiomas
) {

    public record DadosPessoais(
            @JsonProperty("nome") String nome,
            @JsonProperty("email") String email,
            @JsonProperty("telefone") String telefone,
            @JsonProperty("linkedin") String linkedin,
            @JsonProperty("localizacao") String localizacao
    ) {}

    public record Experiencia(
            @JsonProperty("cargo") String cargo,
            @JsonProperty("empresa") String empresa,
            @JsonProperty("data_inicio") String dataInicio,
            @JsonProperty("data_fim") String dataFim,
            @JsonProperty("atual") boolean atual,
            @JsonProperty("bullets") List<String> bullets
    ) {}

    public record Educacao(
            @JsonProperty("curso") String curso,
            @JsonProperty("instituicao") String instituicao,
            @JsonProperty("data_inicio") String dataInicio,
            @JsonProperty("data_fim") String dataFim
    ) {}

    /**
     * Skills preserve the grouping of the original resume (e.g. "Linguagens",
     * "Frameworks / Libs"); a resume without grouped skills yields a single
     * group with an empty {@code categoria}.
     */
    public record SkillGroup(
            @JsonProperty("categoria") String categoria,
            @JsonProperty("itens") List<String> itens
    ) {}

    public record Certificacao(
            @JsonProperty("nome") String nome,
            @JsonProperty("instituicao") String instituicao,
            @JsonProperty("data") String data
    ) {}

    public record Idioma(
            @JsonProperty("idioma") String idioma,
            @JsonProperty("nivel") String nivel
    ) {}
}
