package taskschedulerself.model;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class Task {
    AtomicInteger generator =new AtomicInteger();
    private int id;
    private Future<Void> future;
    private TaskStaus taskStaus;
    private int attemptCount;

    public Task( Future<Void> future, TaskStaus taskStaus, int attemptCount) {
        this.id = generator.getAndIncrement();
        this.future = future;
        this.taskStaus = taskStaus;
        this.attemptCount = attemptCount;
    }

    public AtomicInteger getGenerator() {
        return generator;
    }

    public void setGenerator(AtomicInteger generator) {
        this.generator = generator;
    }

    public int getId() {
        return id;
    }


    public Future<Void> getFuture() {
        return future;
    }

    public void setFuture(Future<Void> future) {
        this.future = future;
    }

    public TaskStaus getTaskStaus() {
        return taskStaus;
    }

    public void setTaskStaus(TaskStaus taskStaus) {
        this.taskStaus = taskStaus;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
}
