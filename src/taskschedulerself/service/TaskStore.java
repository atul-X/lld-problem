package taskschedulerself.service;

import taskschedulerself.model.Task;
import taskschedulerself.model.TaskStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

// Everything about "what tasks exist and what's their state" lives here. Two maps keyed by
// task id: one holds the Task itself (name/job/status/attempts), the other holds whichever
// ScheduledFuture currently represents that task's pending run, so cancel() can call
// future.cancel() instead of the executor needing to know about cancellation at all.
class TaskStore {
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingRuns = new ConcurrentHashMap<>();

    void save(Task task) {
        tasks.put(task.getId(), task);
    }

    Task get(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("unknown taskId " + taskId);
        }
        return task;
    }

    void trackPendingRun(String taskId, ScheduledFuture<?> future) {
        pendingRuns.put(taskId, future);
    }

    TaskStatus getStatus(String taskId) {
        return get(taskId).getStatus();
    }

    boolean cancel(String taskId) {
        Task task = get(taskId);
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return false;
        }
        ScheduledFuture<?> future = pendingRuns.get(taskId);
        if (future != null) {
            future.cancel(false);
        }
        task.setStatus(TaskStatus.CANCELLED);
        return true;
    }
}
