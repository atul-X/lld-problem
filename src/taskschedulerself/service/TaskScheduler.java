package taskschedulerself.service;

import taskschedulerself.model.TaskRequest;
import taskschedulerself.model.TaskStatus;

public interface TaskScheduler {
    String submit(TaskRequest taskRequest);
    boolean cancel(String taskId);
    TaskStatus getStatus(String taskId);
    void shutdown();
}
