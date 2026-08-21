package taskscheduler.service;

import taskscheduler.model.Task;
import taskscheduler.model.TaskStatus;

public interface TaskListener {

    void onStatusChanged(Task task, TaskStatus oldStatus, TaskStatus newStatus);
}
