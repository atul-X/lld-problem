package taskschedulerself;

import taskschedulerself.model.ExponentialRetry;
import taskschedulerself.model.FixedDelayStrategy;
import taskschedulerself.model.TaskRequest;
import taskschedulerself.service.InMemoryTaskScheduler;
import taskschedulerself.service.TaskScheduler;

import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TaskScheduler scheduler = new InMemoryTaskScheduler(4);

        // one-time task
        String oneTimeId = scheduler.submit(new TaskRequest.Builder()
                .name("say-hello")
                .job(() -> {
                    System.out.println("hello from a one-time task");
                    return null;
                })
                .initialDelayMillis(100)
                .build());

        // fails twice, then succeeds, using exponential backoff retry
        AtomicInteger attempts = new AtomicInteger();
        String retryId = scheduler.submit(new TaskRequest.Builder()
                .name("flaky-task")
                .job(() -> {
                    int attempt = attempts.incrementAndGet();
                    if (attempt < 3) {
                        throw new RuntimeException("simulated failure #" + attempt);
                    }
                    System.out.println("flaky task finally succeeded on attempt " + attempt);
                    return null;
                })
                .retryPolicy(new ExponentialRetry(5, 100))
                .build());

        // recurring task, cancelled after a couple of runs
        AtomicInteger recurringRuns = new AtomicInteger();
        String recurringId = scheduler.submit(new TaskRequest.Builder()
                .name("heartbeat")
                .job(() -> {
                    System.out.println("heartbeat #" + recurringRuns.incrementAndGet());
                    return null;
                })
                .recurrenceStrategy(new FixedDelayStrategy(200))
                .build());

        Thread.sleep(700);
        boolean cancelled = scheduler.cancel(recurringId);
        System.out.println("cancelled heartbeat: " + cancelled);

        Thread.sleep(500);
        System.out.println("one-time status: " + scheduler.getStatus(oneTimeId));
        System.out.println("flaky status: " + scheduler.getStatus(retryId));
        System.out.println("heartbeat status: " + scheduler.getStatus(recurringId));
        System.out.println("heartbeat total runs: " + recurringRuns.get());

        scheduler.shutdown();
    }
}
