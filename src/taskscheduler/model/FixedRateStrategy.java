package taskscheduler.model;

public class FixedRateStrategy implements RecurrenceStrategy {

    private final long periodMillis;

    public FixedRateStrategy(long periodMillis) {
        if (periodMillis <= 0) {
            throw new IllegalArgumentException("periodMillis must be positive");
        }
        this.periodMillis = periodMillis;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    // anchored to the previous scheduled time, not completion time, so a slow run doesn't push later runs out
    @Override
    public long nextExecutionTimeMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return lastScheduledTimeMillis + periodMillis;
    }
}
