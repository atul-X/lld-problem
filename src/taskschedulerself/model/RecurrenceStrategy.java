package taskschedulerself.model;

public interface RecurrenceStrategy {
    Boolean isRecurring();
    long nextExecutionTimeInMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis);
}
