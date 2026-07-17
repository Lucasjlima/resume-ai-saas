package saas.com.br.resume_ai_saas.regeneration.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import saas.com.br.resume_ai_saas.regeneration.exception.LatexCompilationException;

import java.io.IOException;

@Component
public class PdfPageCounter {

    public int count(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new LatexCompilationException("Failed to inspect the generated PDF", e);
        }
    }
}
