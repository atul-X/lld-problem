package taskscheduler.model;

public class OneTimeStrategy implements RecurrenceStrategy {

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public long nextExecutionTimeMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis) {
        return -1;
    }
}
