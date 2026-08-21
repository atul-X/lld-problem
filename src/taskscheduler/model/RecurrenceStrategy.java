package taskscheduler.model;

public interface RecurrenceStrategy {

    boolean isRecurring();

    /**
     * @param lastScheduledTimeMillis  the execution time that was scheduled for the run that just finished
     * @param lastCompletionTimeMillis wall-clock time the run actually finished
     * @return next execution time in epoch millis, or -1 if there is no next occurrence
     */
    long nextExecutionTimeMillis(long lastScheduledTimeMillis, long lastCompletionTimeMillis);
}
