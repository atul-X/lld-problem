package taskschedulerself.model;

// "fixed delay": next run starts `delayMillis` after the PREVIOUS run finished.
public class FixedDelayStrategy implements RecurrenceStrategy {
    private final long delayMillis;

    public FixedDelayStrategy(long delayMillis) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must not be negative");
        }
        this.delayMillis = delayMillis;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return lastCompletionTimeMillis + delayMillis;
    }
}
