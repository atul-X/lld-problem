package taskschedulerself.model;

import java.util.UUID;
import java.util.concurrent.Callable;

public class Task {
    private final String id;
    private final String name;
    private final Callable<Void> job;
    private final RetryPolicy retryPolicy;
    private final RecurrenceStrategy recurrenceStrategy;

    private volatile TaskStatus status = TaskStatus.PENDING;
    private volatile int attemptCount = 0;

    public Task(String name, Callable<Void> job, RetryPolicy retryPolicy, RecurrenceStrategy recurrenceStrategy) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.job = job;
        this.retryPolicy = retryPolicy;
        this.recurrenceStrategy = recurrenceStrategy;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Callable<Void> getJob() {
        return job;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public RecurrenceStrategy getRecurrenceStrategy() {
        return recurrenceStrategy;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int incrementAttempt() {
        return ++attemptCount;
    }

    public void resetAttempts() {
        attemptCount = 0;
    }
}
