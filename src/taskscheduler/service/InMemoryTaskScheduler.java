package taskscheduler.service;

import taskscheduler.exception.TaskSchedulerException;
import taskscheduler.model.RetryPolicy;
import taskscheduler.model.ScheduledTaskWrapper;
import taskscheduler.model.Task;
import taskscheduler.model.TaskRequest;
import taskscheduler.model.TaskStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InMemoryTaskScheduler implements TaskScheduler {

    // DelayQueue.take() blocks until the head's delay has elapsed, so it doubles as both the
    // ready-work queue and the "sleep until due" mechanism - no separate timer thread needed.
    // It's also safe for multiple concurrent consumers, which is exactly how each worker below
    // pulls its own work directly off it (the same trick ScheduledThreadPoolExecutor's internal
    // DelayedWorkQueue uses).
    private final DelayQueue<ScheduledTaskWrapper> readyQueue = new DelayQueue<>();

    // id -> the wrapper currently "live" for that task, so cancel()/getStatus() are O(1)
    // instead of scanning the queue.
    private final Map<String, ScheduledTaskWrapper> taskRegistry = new ConcurrentHashMap<>();

    private final List<TaskListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService workerPool;
    private volatile boolean running = true;

    public InMemoryTaskScheduler(int workerPoolSize) {
        if (workerPoolSize < 1) {
            throw new IllegalArgumentException("workerPoolSize must be at least 1");
        }
        this.workerPool = Executors.newFixedThreadPool(workerPoolSize);
        for (int i = 0; i < workerPoolSize; i++) {
            workerPool.submit(this::workerLoop);
        }
    }

    @Override
    public String submit(TaskRequest request) {
        if (!running) {
            throw new TaskSchedulerException("scheduler is shut down, rejecting task " + request.getName());
        }
        Task task = new Task(request.getName(), request.getJob(), request.getRetryPolicy());
        long executionTime = System.currentTimeMillis() + request.getInitialDelayMillis();
        ScheduledTaskWrapper wrapper = new ScheduledTaskWrapper(task, request.getRecurrenceStrategy(), executionTime);
        register(wrapper, TaskStatus.SCHEDULED);
        return task.getId();
    }

    @Override
    public boolean cancel(String taskId) {
        ScheduledTaskWrapper wrapper = taskRegistry.get(taskId);
        if (wrapper == null || wrapper.isCancelled()) {
            return false;
        }
        wrapper.cancel();
        updateStatus(wrapper.getTask(), TaskStatus.CANCELLED);
        return true;
    }

    @Override
    public TaskStatus getStatus(String taskId) {
        ScheduledTaskWrapper wrapper = taskRegistry.get(taskId);
        if (wrapper == null) {
            throw new TaskSchedulerException("unknown taskId " + taskId);
        }
        return wrapper.getTask().getStatus();
    }

    @Override
    public void addListener(TaskListener listener) {
        listeners.add(listener);
    }

    @Override
    public void shutdown() {
        running = false;
        workerPool.shutdownNow();
    }

    private void register(ScheduledTaskWrapper wrapper, TaskStatus status) {
        taskRegistry.put(wrapper.getTask().getId(), wrapper);
        updateStatus(wrapper.getTask(), status);
        readyQueue.put(wrapper);
    }

    private void workerLoop() {
        while (running) {
            try {
                ScheduledTaskWrapper wrapper = readyQueue.take();
                if (wrapper.isCancelled()) {
                    continue;
                }
                execute(wrapper);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void execute(ScheduledTaskWrapper wrapper) {
        Task task = wrapper.getTask();
        // a cancel() racing in right here is fine: it flips status to CANCELLED and we
        // immediately overwrite it with RUNNING, then the real outcome below wins - cancellation
        // of an in-flight run is best-effort, same contract as Future.cancel(false).
        updateStatus(task, TaskStatus.RUNNING);
        try {
            task.getJob().call();
            task.resetAttempts();
            updateStatus(task, TaskStatus.COMPLETED);
            rescheduleIfRecurring(wrapper);
        } catch (Exception e) {
            handleFailure(wrapper, e);
        }
    }

    private void handleFailure(ScheduledTaskWrapper wrapper, Exception failure) {
        Task task = wrapper.getTask();
        int attempt = task.incrementAttempt();
        RetryPolicy retryPolicy = task.getRetryPolicy();

        if (wrapper.isCancelled()) {
            return;
        }
        if (retryPolicy.shouldRetry(attempt, failure)) {
            long nextRun = System.currentTimeMillis() + retryPolicy.backoffMillis(attempt);
            register(new ScheduledTaskWrapper(task, wrapper.getRecurrenceStrategy(), nextRun), TaskStatus.SCHEDULED);
        } else {
            updateStatus(task, TaskStatus.FAILED);
            task.resetAttempts();
            rescheduleIfRecurring(wrapper);
        }
    }

    private void rescheduleIfRecurring(ScheduledTaskWrapper wrapper) {
        if (!wrapper.getRecurrenceStrategy().isRecurring()) {
            return;
        }
        ScheduledTaskWrapper next = wrapper.nextOccurrence(System.currentTimeMillis());
        if (next != null && !wrapper.isCancelled()) {
            register(next, TaskStatus.SCHEDULED);
        }
    }

    private void updateStatus(Task task, TaskStatus newStatus) {
        TaskStatus old = task.getStatus();
        task.setStatus(newStatus);
        for (TaskListener listener : listeners) {
            listener.onStatusChanged(task, old, newStatus);
        }
    }
}
