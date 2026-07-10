package saas.com.br.resume_ai_saas.regeneration.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;
import saas.com.br.resume_ai_saas.regeneration.exception.LatexTemplateException;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static saas.com.br.resume_ai_saas.regeneration.service.LatexEscaper.escape;

/**
 * Populates the fixed LaTeX template with a {@link GeneratedResume}. All
 * string values are LaTeX-escaped here, so the template interpolates them
 * as-is.
 */
@Service
public class LatexTemplateService {

    private static final String TEMPLATE_NAME = "resume.tex.ftl";

    private final Configuration freemarker;

    public LatexTemplateService() {
        this.freemarker = new Configuration(Configuration.VERSION_2_3_33);
        this.freemarker.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates/latex");
        this.freemarker.setDefaultEncoding("UTF-8");
    }

    /**
     * @param compact denser layout (smaller font, margins and spacing) used
     *                as fallback when the regular layout overflows one page.
     */
    public String render(GeneratedResume resume, boolean compact) {
        try {
            Template template = freemarker.getTemplate(TEMPLATE_NAME);
            StringWriter out = new StringWriter();
            template.process(buildModel(resume, compact), out);
            return out.toString();
        } catch (Exception e) {
            throw new LatexTemplateException("Failed to render LaTeX template", e);
        }
    }

    private Map<String, Object> buildModel(GeneratedResume resume, boolean compact) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("compact", compact);
        GeneratedResume.DadosPessoais dados = resume.dadosPessoais();

        model.put("nome", escape(dados.nome()));
        model.put("contato", buildContactLine(dados));
        model.put("resumo", escape(resume.resumoProfissional()));

        model.put("experiencias", resume.experiencias().stream().map(exp -> Map.of(
                "cargo", escape(exp.cargo()),
                "empresa", escape(exp.empresa()),
                "periodo", buildPeriod(exp.dataInicio(), exp.dataFim(), exp.atual()),
                "bullets", exp.bullets() == null ? List.of() : exp.bullets().stream().map(LatexEscaper::escape).toList()
        )).toList());

        model.put("educacao", resume.educacao().stream().map(edu -> Map.of(
                "curso", escape(edu.curso()),
                "instituicao", escape(edu.instituicao()),
                "periodo", buildPeriod(edu.dataInicio(), edu.dataFim(), false)
        )).toList());

        model.put("skills", resume.skills().stream().map(LatexEscaper::escape).toList());

        model.put("certificacoes", resume.certificacoes().stream().map(cert -> Map.of(
                "nome", escape(cert.nome()),
                "instituicao", escape(cert.instituicao()),
                "data", escape(cert.data())
        )).toList());

        model.put("idiomas", resume.idiomas().stream().map(idioma -> Map.of(
                "idioma", escape(idioma.idioma()),
                "nivel", escape(idioma.nivel())
        )).toList());

        return model;
    }

    private String buildContactLine(GeneratedResume.DadosPessoais dados) {
        List<String> parts = new ArrayList<>();
        for (String part : List.of(escape(dados.email()), escape(dados.telefone()),
                escape(dados.linkedin()), escape(dados.localizacao()))) {
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return String.join(" \\textbullet{} ", parts);
    }

    /**
     * Dates are free-form strings, exactly as extracted from the original
     * resume — no parsing or normalization (spec rule).
     */
    private String buildPeriod(String start, String end, boolean current) {
        String escapedStart = escape(start);
        String escapedEnd = current ? "Atual" : escape(end);
        if (escapedStart.isBlank()) {
            return escapedEnd;
        }
        if (escapedEnd.isBlank()) {
            return escapedStart;
        }
        return escapedStart + " -- " + escapedEnd;
    }
}
