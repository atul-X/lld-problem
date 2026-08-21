package taskschedulerself.model;

public class FixedRateStrategy implements RecurrenceStrategy{
    private int  periodMillis;

    public FixedRateStrategy(int periodMillis) {
        if (periodMillis<0){
            throw new IllegalArgumentException("periodMillis must be positive");
        }
        this.periodMillis = periodMillis;
    }

    @Override
    public Boolean isRecurring() {
        return true;
    }

    @Override
    public long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return lastScheduledTimeMillis*periodMillis;
    }
}
