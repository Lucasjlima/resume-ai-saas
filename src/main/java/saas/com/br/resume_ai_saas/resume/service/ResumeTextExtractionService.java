package saas.com.br.resume_ai_saas.resume.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeTextExtractionService {

    private static final String[][] LIGATURE_WORD_FIXES = {
            {"certifcat", "certificat"},
            {"specifcat", "specificat"},
            {"signifcan", "significan"},
            {"confgur", "configur"},
            {"effcien", "efficien"},
            {"notifcat", "notificat"},
            {"verifcat", "verificat"},
            {"classifcat", "classificat"},
            {"identifcat", "identificat"},
            {"profle", "profile"},
            {"\\bfle\\b", "file"},
            {"\\bfles\\b", "files"},
            {"\\bflter", "filter"},
            {"workfow", "workflow"},
            {"\\bfuenc", "fluenc"},
            {"\\bfuent", "fluent"},
            {"confict", "conflict"},
            {"infuenc", "influenc"},
            {"refect(?!ed\\b|ion\\b|ive\\b)", "reflect"},
    };

    public String extract(byte[] bytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        List<Document> documents = reader.get();
        String raw = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        return sanitize(raw);
    }

    private String sanitize(String raw) {
        String text = raw;
        text = replaceUnicodeLigatures(text);
        text = fixDroppedLigatures(text);
        text = stripMojibake(text);
        text = normalizeWhitespace(text);
        return text;
    }

    private String replaceUnicodeLigatures(String text) {
        return text.replace("ﬀ", "ff")
                .replace("ﬁ", "fi")
                .replace("ﬂ", "fl")
                .replace("ﬃ", "ffi")
                .replace("ﬄ", "ffl");
    }

    private String fixDroppedLigatures(String text) {
        for (String[] fix : LIGATURE_WORD_FIXES) {
            text = text.replaceAll("(?i)" + fix[0], fix[1]);
        }
        return text;
    }

    private String stripMojibake(String text) {
        text = text.replaceAll("[§ƒï†‡¶]+\\s*", "");
        text = text.replaceAll("#\\s*(?=\\S+@\\S+)", "");
        text = text.replaceAll("\\+\\s+(?=[A-Z])", "");
        text = text.replaceAll("\\s*\\|\\s*", " ");
        return text;
    }

    private String normalizeWhitespace(String text) {
        text = text.replaceAll("[ \\t]{2,}", " ");
        text = text.replaceAll("(\\r?\\n){3,}", "\n\n");
        text = text.lines()
                .map(String::strip)
                .collect(Collectors.joining("\n"));
        return text.strip();
    }
}
