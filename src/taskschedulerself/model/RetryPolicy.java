package taskschedulerself.model;

public interface RetryPolicy {
    boolean shouldRetry(int attemptCount, Exception exception);
    long backoffMillis(int attemptCount);
}
