package ratelimiter.service;

public class RateLimiterException extends  Exception{
    private int retryAfterMillies;

    public RateLimiterException(int retryAfterMillies) {
        this.retryAfterMillies = retryAfterMillies;
    }

    public RateLimiterException(String message, int retryAfterMillies) {
        super(message);
        this.retryAfterMillies = retryAfterMillies;
    }

    public RateLimiterException(String message, Throwable cause, int retryAfterMillies) {
        super(message, cause);
        this.retryAfterMillies = retryAfterMillies;
    }

    public int getRetryAfterMillies() {
        return retryAfterMillies;
    }
}
