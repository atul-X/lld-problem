package taskschedulerself.model;

public class FixedDelayStrategy  implements RecurrenceStrategy{
    private int  delayMillis;

    public FixedDelayStrategy(int delayMillis) {
        if (delayMillis<0){
            throw new IllegalArgumentException("periodMillis must be positive");
        }
        this.delayMillis = delayMillis;
    }

    @Override
    public Boolean isRecurring() {
        return true;
    }

    @Override
    public long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return lastCompletionTimeMillis*delayMillis;
    }
}
