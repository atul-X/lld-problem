package taskschedulerself.model;

public interface RetryPolicy {
    boolean shouldRetry(int attemptCount,Exception exception);
    int backOfMillis(int attemptCount);
}
