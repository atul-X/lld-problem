package taskschedulerself.service;

import taskschedulerself.model.RecurrenceStrategy;
import taskschedulerself.model.RetryPolicy;
import taskschedulerself.model.Task;
import taskschedulerself.model.TaskStatus;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Runs tasks. ScheduledExecutorService already does the "wait until due, then hand to a
// worker thread" part, so there's no queue/thread-loop to hand-roll here - schedule() just
// asks it to run the task once after a delay, and retry/recurrence both work by scheduling
// another one-shot run when the previous one finishes.
class TaskExecutor {
    private final ScheduledExecutorService workerPool;
    private final TaskStore taskStore;

    TaskExecutor(int workerPoolSize, TaskStore taskStore) {
        this.workerPool = Executors.newScheduledThreadPool(workerPoolSize);
        this.taskStore = taskStore;
    }


    void schedule(Task task, long delayMillis) {
        long scheduledTime = System.currentTimeMillis() + Math.max(delayMillis, 0);
        ScheduledFuture<?> future = workerPool.schedule(
                () -> run(task, scheduledTime), delayMillis, TimeUnit.MILLISECONDS);
        taskStore.trackPendingRun(task.getId(), future);
        task.setStatus(TaskStatus.SCHEDULED);
    }

    private void run(Task task, long scheduledTime) {
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        task.setStatus(TaskStatus.RUNNING);
        try {
            task.getJob().call();
            task.setStatus(TaskStatus.COMPLETED);
            task.resetAttempts();
            rescheduleIfRecurring(task, scheduledTime, System.currentTimeMillis());
        } catch (Exception e) {
            handleFailure(task, scheduledTime, e);
        }
    }

    private void handleFailure(Task task, long scheduledTime, Exception failure) {
        int attempt = task.incrementAttempt();
        RetryPolicy retryPolicy = task.getRetryPolicy();
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        if (retryPolicy.shouldRetry(attempt, failure)) {
            schedule(task, retryPolicy.backoffMillis(attempt));
        } else {
            task.setStatus(TaskStatus.FAILED);
            task.resetAttempts();
            rescheduleIfRecurring(task, scheduledTime, System.currentTimeMillis());
        }
    }

    private void rescheduleIfRecurring(Task task, long scheduledTime, long completionTime) {
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        RecurrenceStrategy recurrenceStrategy = task.getRecurrenceStrategy();
        if (!recurrenceStrategy.isRecurring()) {
            return;
        }
        long next = recurrenceStrategy.nextExecutionTimeInMillis(scheduledTime, completionTime);
        if (next < 0) {
            return;
        }
        schedule(task, next - System.currentTimeMillis());
    }

    void shutdown() {
        workerPool.shutdownNow();
    }




}
