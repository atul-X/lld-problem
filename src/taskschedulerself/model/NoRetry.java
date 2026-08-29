package taskschedulerself.model;

public class NoRetry implements RetryPolicy {
    @Override
    public boolean shouldRetry(int attemptCount, Exception exception) {
        return false;
    }

    @Override
    public long backoffMillis(int attemptCount) {
        return 0;
    }
}
