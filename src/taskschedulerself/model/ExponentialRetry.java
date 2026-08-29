package taskschedulerself.model;

public class ExponentialRetry implements RetryPolicy {
    private final int maxAttempts;
    private final long baseDelayMillis;

    public ExponentialRetry(int maxAttempts, long baseDelayMillis) {
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
    public boolean shouldRetry(int attemptCount, Exception exception) {
        return attemptCount < maxAttempts;
    }

    @Override
    public long backoffMillis(int attemptCount) {
        int cappedAttempt = Math.min(attemptCount, 10);
        return baseDelayMillis * (1L << cappedAttempt);
    }
}
