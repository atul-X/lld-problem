package taskscheduler.model;

import java.util.UUID;
import java.util.concurrent.Callable;

public class Task {

    private final String id;
    private final String name;
    private final Callable<Void> job;
    private final RetryPolicy retryPolicy;

    // volatile, not synchronized: at most one worker thread ever touches a given task's mutable
    // state at a time (a task is only re-enqueued after its previous run fully finishes), so plain
    // visibility is enough - no compound read-modify-write races across threads to guard against.
    private volatile TaskStatus status = TaskStatus.PENDING;
    private volatile int attemptCount = 0;

    public Task(String name, Callable<Void> job, RetryPolicy retryPolicy) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.job = job;
        this.retryPolicy = retryPolicy;
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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int incrementAttempt() {
        return ++attemptCount;
    }

    public void resetAttempts() {
        attemptCount = 0;
    }
}
