package taskschedulerself.model;

public class NoRetry implements RetryPolicy {
    @Override
    public boolean shouldRetry(int attemptCount, Exception exception) {
        return false;
    }

    @Override
    public int backOfMillis(int attemptCount) {
        return 0;
    }
}
