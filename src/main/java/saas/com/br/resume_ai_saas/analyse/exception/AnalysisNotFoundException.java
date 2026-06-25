package saas.com.br.resume_ai_saas.analyse.exception;

public class AnalysisNotFoundException extends RuntimeException {
    public AnalysisNotFoundException(Long id) {
        super("Analysis not found with id: " + id);
    }
}
