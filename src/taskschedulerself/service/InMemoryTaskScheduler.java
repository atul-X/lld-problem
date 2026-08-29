package taskschedulerself.service;

import taskschedulerself.model.Task;
import taskschedulerself.model.TaskRequest;
import taskschedulerself.model.TaskStatus;

// Facade: TaskStore owns "what tasks exist and what state are they in", TaskExecutor owns
// "how do we actually run one". This class just wires a request into a Task, hands it to
// both, and forwards lookups/cancellation to the store.
public class InMemoryTaskScheduler implements TaskScheduler {
    private final TaskStore taskStore = new TaskStore();
    private final TaskExecutor taskExecutor;

    public InMemoryTaskScheduler(int workerPoolSize) {
        if (workerPoolSize < 1) {
            throw new IllegalArgumentException("workerPoolSize must be at least 1");
        }
        this.taskExecutor = new TaskExecutor(workerPoolSize, taskStore);
    }

    @Override
    public String submit(TaskRequest taskRequest) {
        Task task = new Task(
                taskRequest.getName(),
                taskRequest.getJob(),
                taskRequest.getRetryPolicy(),
                taskRequest.getRecurrenceStrategy());
        taskStore.save(task);
        taskExecutor.schedule(task, taskRequest.getInitialDelayMillis());
        return task.getId();
    }

    @Override
    public boolean cancel(String taskId) {
        return taskStore.cancel(taskId);
    }

    @Override
    public TaskStatus getStatus(String taskId) {
        return taskStore.getStatus(taskId);
    }

    @Override
    public void shutdown() {
        taskExecutor.shutdown();
    }
}
