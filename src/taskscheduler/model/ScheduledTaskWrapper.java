package taskscheduler.model;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// One occurrence of a Task sitting in the DelayQueue. Recurring tasks get a fresh wrapper
// per occurrence (see nextOccurrence) rather than mutating this one in place, so a wrapper
// already inside the queue's heap is never mutated after insertion - only the queue's own
// synchronized re-heapify (on take()/offer()) touches ordering-relevant state.
public class ScheduledTaskWrapper implements Delayed {

    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong();

    private final Task task;
    private final RecurrenceStrategy recurrenceStrategy;
    private final long executionTimeMillis;
    private final long sequenceNumber;

    // lazy-cancellation flag: DelayQueue has no cheap way to remove an arbitrary element, so
    // cancel() just flags the wrapper and the worker skips it when it's popped instead.
    private volatile boolean cancelled = false;

    public ScheduledTaskWrapper(Task task, RecurrenceStrategy recurrenceStrategy, long executionTimeMillis) {
        this.task = task;
        this.recurrenceStrategy = recurrenceStrategy;
        this.executionTimeMillis = executionTimeMillis;
        this.sequenceNumber = SEQUENCE_GENERATOR.getAndIncrement();
    }

    public Task getTask() {
        return task;
    }

    public RecurrenceStrategy getRecurrenceStrategy() {
        return recurrenceStrategy;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public ScheduledTaskWrapper nextOccurrence(long completionTimeMillis) {
        long next = recurrenceStrategy.nextExecutionTimeMillis(executionTimeMillis, completionTimeMillis);
        if (next < 0) {
            return null;
        }
        return new ScheduledTaskWrapper(task, recurrenceStrategy, next);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(executionTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        if (other instanceof ScheduledTaskWrapper) {
            ScheduledTaskWrapper that = (ScheduledTaskWrapper) other;
            int cmp = Long.compare(this.executionTimeMillis, that.executionTimeMillis);
            return cmp != 0 ? cmp : Long.compare(this.sequenceNumber, that.sequenceNumber);
        }
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
}
