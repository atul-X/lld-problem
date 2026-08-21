package taskscheduler.model;

public class ExponentialBackoffRetryPolicy implements RetryPolicy {

    private final int maxAttempts;
    private final long baseDelayMillis;

    public ExponentialBackoffRetryPolicy(int maxAttempts, long baseDelayMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (baseDelayMillis <= 0) {
            throw new IllegalArgumentException("baseDelayMillis must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelayMillis = baseDelayMillis;
    }

    @Override
    public boolean shouldRetry(int attemptCount, Exception lastFailure) {
        return attemptCount < maxAttempts;
    }

    @Override
    public long backoffMillis(int attemptCount) {
        // capped at 2^10 so a runaway attempt count can't overflow the shift
        int cappedAttempt = Math.min(attemptCount, 10);
        return baseDelayMillis * (1L << cappedAttempt);
    }
}
