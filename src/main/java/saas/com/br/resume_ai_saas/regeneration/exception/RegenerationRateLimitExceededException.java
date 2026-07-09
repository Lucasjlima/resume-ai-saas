package saas.com.br.resume_ai_saas.regeneration.exception;

public class RegenerationRateLimitExceededException extends RuntimeException {
    public RegenerationRateLimitExceededException(int dailyLimit) {
        super("Daily regeneration limit of " + dailyLimit + " reached. Try again later.");
    }
}
