package taskscheduler.model;

public interface RetryPolicy {

    boolean shouldRetry(int attemptCount, Exception lastFailure);

    long backoffMillis(int attemptCount);
}
