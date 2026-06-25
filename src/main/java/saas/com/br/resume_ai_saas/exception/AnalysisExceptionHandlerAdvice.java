package saas.com.br.resume_ai_saas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import saas.com.br.resume_ai_saas.analyse.exception.AnalysisNotFoundException;

@RestControllerAdvice
public class AnalysisExceptionHandlerAdvice {

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnalysisNotFound(AnalysisNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage(), 404));
    }
}
