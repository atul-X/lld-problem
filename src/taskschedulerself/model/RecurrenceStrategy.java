package taskschedulerself.model;

public interface RecurrenceStrategy {
    boolean isRecurring();
    long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis);
}
