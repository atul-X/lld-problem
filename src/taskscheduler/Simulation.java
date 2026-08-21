package taskscheduler;

import taskscheduler.model.ExponentialBackoffRetryPolicy;
import taskscheduler.model.FixedRateStrategy;
import taskscheduler.model.TaskRequest;
import taskscheduler.service.InMemoryTaskScheduler;
import taskscheduler.service.TaskScheduler;

import java.util.concurrent.atomic.AtomicInteger;

public class Simulation {

    public static void main(String[] args) throws InterruptedException {
        TaskScheduler scheduler = new InMemoryTaskScheduler(3);
        scheduler.addListener((task, oldStatus, newStatus) ->
                System.out.printf("[listener] %s: %s -> %s%n", task.getName(), oldStatus, newStatus));

        runOneTimeTask(scheduler);
        runRecurringTaskThenCancel(scheduler);
        runFlakyTaskWithRetries(scheduler);
        runCancelBeforeItFires(scheduler);

        Thread.sleep(500);
        scheduler.shutdown();
    }

    private static void runOneTimeTask(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("=== one-time task, fires after 200ms ===");
        String taskId = scheduler.submit(TaskRequest.builder()
                .name("welcome-email")
                .initialDelayMillis(200)
                .job(() -> {
                    System.out.println("sending welcome email");
                    return null;
                })
                .build());
        Thread.sleep(400);
        System.out.println("final status: " + scheduler.getStatus(taskId));
        System.out.println();
    }

    private static void runRecurringTaskThenCancel(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("=== fixed-rate task every 150ms, cancelled after ~3 runs ===");
        AtomicInteger runs = new AtomicInteger();
        String taskId = scheduler.submit(TaskRequest.builder()
                .name("heartbeat")
                .initialDelayMillis(0)
                .recurrenceStrategy(new FixedRateStrategy(150))
                .job(() -> {
                    System.out.println("heartbeat #" + runs.incrementAndGet());
                    return null;
                })
                .build());
        Thread.sleep(500);
        boolean cancelled = scheduler.cancel(taskId);
        System.out.println("cancel requested: " + cancelled + ", total runs observed: " + runs.get());
        Thread.sleep(300);
        System.out.println("runs after cancel settled: " + runs.get());
        System.out.println();
    }

    private static void runFlakyTaskWithRetries(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("=== flaky task, fails twice then succeeds, exponential backoff ===");
        AtomicInteger callCount = new AtomicInteger();
        scheduler.submit(TaskRequest.builder()
                .name("flaky-webhook")
                .initialDelayMillis(0)
                .retryPolicy(new ExponentialBackoffRetryPolicy(5, 50))
                .job(() -> {
                    int attempt = callCount.incrementAndGet();
                    if (attempt <= 2) {
                        throw new RuntimeException("webhook timeout, attempt " + attempt);
                    }
                    System.out.println("webhook delivered on attempt " + attempt);
                    return null;
                })
                .build());
        Thread.sleep(600);
        System.out.println();
    }

    private static void runCancelBeforeItFires(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("=== task cancelled before its delay elapses, should never run ===");
        AtomicInteger executed = new AtomicInteger();
        String taskId = scheduler.submit(TaskRequest.builder()
                .name("stale-report")
                .initialDelayMillis(300)
                .job(() -> {
                    executed.incrementAndGet();
                    System.out.println("stale-report ran (should not happen)");
                    return null;
                })
                .build());
        scheduler.cancel(taskId);
        Thread.sleep(400);
        System.out.println("executed count: " + executed.get() + ", status: " + scheduler.getStatus(taskId));
        System.out.println();
    }
}
