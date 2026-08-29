package taskschedulerself.model;

public class OneTimeStrategy implements RecurrenceStrategy {
    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return -1;
    }
}
