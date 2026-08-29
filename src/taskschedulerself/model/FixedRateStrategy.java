package taskschedulerself.model;

// "fixed rate": next run is `periodMillis` after the PREVIOUS run was scheduled to start
// (regardless of how long that run actually took).
public class FixedRateStrategy implements RecurrenceStrategy {
    private final long periodMillis;

    public FixedRateStrategy(long periodMillis) {
        if (periodMillis < 0) {
            throw new IllegalArgumentException("periodMillis must not be negative");
        }
        this.periodMillis = periodMillis;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return lastScheduledTimeMillis + periodMillis;
    }
}
