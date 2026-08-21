package taskscheduler.service;

import taskscheduler.model.TaskRequest;
import taskscheduler.model.TaskStatus;

public interface TaskScheduler {

    String submit(TaskRequest request);

    boolean cancel(String taskId);

    TaskStatus getStatus(String taskId);

    void addListener(TaskListener listener);

    void shutdown();
}
