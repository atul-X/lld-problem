package taskscheduler.model;

public class NoRetryPolicy implements RetryPolicy {

    @Override
    public boolean shouldRetry(int attemptCount, Exception lastFailure) {
        return false;
    }

    @Override
    public long backoffMillis(int attemptCount) {
        return 0;
    }
}
