package saas.com.br.resume_ai_saas.regeneration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import saas.com.br.resume_ai_saas.regeneration.exception.LatexCompilationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Compiles a {@code .tex} source into PDF bytes by invoking the configured
 * LaTeX toolchain ({@code tectonic} by default; any pdflatex-compatible
 * binary that accepts the file as last argument works). Each compilation
 * runs in its own temp directory, removed afterwards.
 */
@Service
@Slf4j
public class LatexCompilationService {

    private static final String TEX_FILE_NAME = "resume.tex";
    private static final String PDF_FILE_NAME = "resume.pdf";
    private static final long TIMEOUT_SECONDS = 120;
    private static final int LOG_TAIL_CHARS = 2000;

    private final String compilerCommand;

    public LatexCompilationService(@Value("${latex.compiler.command:tectonic}") String compilerCommand) {
        this.compilerCommand = compilerCommand;
    }

    public byte[] compile(String texSource) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("latex-regeneration-");
            Path texFile = workDir.resolve(TEX_FILE_NAME);
            Files.writeString(texFile, texSource, StandardCharsets.UTF_8);

            Process process = new ProcessBuilder(compilerCommand, TEX_FILE_NAME)
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new LatexCompilationException("LaTeX compilation timed out after " + TIMEOUT_SECONDS + "s");
            }
            if (process.exitValue() != 0) {
                throw new LatexCompilationException(
                        "LaTeX compiler exited with code " + process.exitValue() + ": " + tail(output));
            }

            Path pdfFile = workDir.resolve(PDF_FILE_NAME);
            if (!Files.exists(pdfFile)) {
                throw new LatexCompilationException("LaTeX compiler produced no PDF: " + tail(output));
            }
            return Files.readAllBytes(pdfFile);
        } catch (IOException e) {
            throw new LatexCompilationException("Failed to run LaTeX compiler '" + compilerCommand + "'", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LatexCompilationException("LaTeX compilation interrupted", e);
        } finally {
            cleanUp(workDir);
        }
    }

    private static String tail(String output) {
        if (output.length() <= LOG_TAIL_CHARS) {
            return output;
        }
        return "..." + output.substring(output.length() - LOG_TAIL_CHARS);
    }

    private static void cleanUp(Path workDir) {
        if (workDir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(workDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            log.warn("Failed to clean LaTeX temp dir {}", workDir, e);
        }
    }
}
