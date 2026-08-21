Design a Task Scheduler

Design an in-process task scheduler: clients submit units of work to run once at a
given time, or repeatedly on a fixed rate / fixed delay, with optional retry on failure.
This is the classic SDE3 LLD prompt because the "trick" isn't the API surface, it's the
concurrency primitive underneath it (see Concurrency Design below) - that's what
separates a working sketch from a design that survives a "how do N threads pull from
the same queue without a lock around the whole thing" follow-up.

Functional Requirements
    -submit a one-time task with an initial delay
    -submit a recurring task: fixed-rate (anchored to schedule) or fixed-delay (anchored to completion)
    -cancel a task, including a still-recurring one (stops future occurrences too)
    -query a task's current status
    -retry a failed task with a pluggable backoff policy
    -observe status transitions (for logging/metrics) without polling

Non-Functional Requirements
    -thread-safe: many producer threads submitting concurrently, a bounded pool of worker
     threads consuming concurrently
    -no busy-waiting - a task due in an hour shouldn't cost any CPU until it's due
    -extensible - new recurrence and retry strategies shouldn't touch the engine
    -best-effort cancellation is acceptable (same contract as Future.cancel(false)):
     an in-flight run isn't interrupted, but no future occurrence will start

Core Entities

    Task
        -id, name, job (Callable<Void>), retryPolicy
        -status (volatile), attemptCount (volatile)
        -mutable state is only ever touched by one worker at a time (a task is never
         re-enqueued until its previous run fully finishes), so volatile is enough -
         no compound read-modify-write races to guard with a lock

    ScheduledTaskWrapper implements Delayed
        -one occurrence of a Task sitting in the queue: task, recurrenceStrategy, executionTimeMillis
        -immutable after construction; a recurring task gets a fresh wrapper per occurrence
         instead of mutating the one already in the queue's heap
        -carries a monotonic sequenceNumber as a tie-breaker so same-millisecond tasks stay FIFO
        -cancel() just flips a volatile flag (lazy cancellation - see below)

    TaskRequest (builder)
        -name, job, initialDelayMillis, recurrenceStrategy (default OneTimeStrategy),
         retryPolicy (default NoRetryPolicy)

    RecurrenceStrategy (Strategy pattern)     -> OneTimeStrategy / FixedRateStrategy / FixedDelayStrategy
    RetryPolicy (Strategy pattern)            -> NoRetryPolicy / ExponentialBackoffRetryPolicy
    TaskListener (Observer pattern)           -> onStatusChanged(task, oldStatus, newStatus)
    TaskStatus                                -> PENDING, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED

service.TaskScheduler (Facade the client talks to)
    design pattern -> Strategy (recurrence + retry) + Observer (listeners) + Command (job as Callable)
    -String submit(TaskRequest)
    -boolean cancel(taskId)
    -TaskStatus getStatus(taskId)
    -void addListener(TaskListener)
    -void shutdown()

service.InMemoryTaskScheduler implements TaskScheduler
    -DelayQueue<ScheduledTaskWrapper> readyQueue
    -Map<String, ScheduledTaskWrapper> taskRegistry   // taskId -> currently-live wrapper, O(1) cancel/status
    -List<TaskListener> listeners                     // CopyOnWriteArrayList
    -ExecutorService workerPool                        // fixed size, each thread runs workerLoop()

Concurrency Design (the part that actually matters for the interview)

    Why DelayQueue instead of a plain PriorityBlockingQueue + a separate timer thread:
    DelayQueue.take() blocks until the head element's delay has elapsed and unblocks
    automatically when a *new* head with a shorter delay is offered (offer() signals the
    waiting consumer). That gives "sleep until due, wake early if something more urgent
    shows up" for free - no separate scheduler thread computing "how long do I sleep"
    and no polling loop. It's the same mechanism java.util.concurrent.ScheduledThreadPoolExecutor
    is built on internally.

    Why N worker threads can all call readyQueue.take() directly, with no lock around the
    "pop and dispatch" step: DelayQueue is internally a PriorityQueue guarded by its own
    lock/condition, and take() atomically pops the head - two threads can never take() the
    same element. So the queue itself *is* the dispatch mechanism; there's no separate
    "scheduler thread hands work to worker threads" step to get wrong. This is deliberately
    the same shape as ScheduledThreadPoolExecutor's DelayedWorkQueue.

    Cancellation is lazy, not eager: DelayQueue has no O(1) way to remove an arbitrary
    element (removal off a heap by identity is O(n)). So cancel() just flips a volatile
    boolean on the wrapper; the worker checks it after take() and silently drops cancelled
    wrappers instead of executing them. Tradeoff: a cancelled-but-not-yet-due wrapper still
    occupies a queue slot until its time arrives. Acceptable for LLD scope; a production
    version would track a "removal count" and periodically compact, similar to
    ScheduledThreadPoolExecutor's removeOnCancelPolicy.

    A cancel() racing with an in-flight run is allowed to lose that race, deliberately:
    execute() unconditionally sets RUNNING, so a CANCELLED status set moments earlier is
    overwritten. The run completes; what cancel() actually guarantees is no *next*
    occurrence gets scheduled (checked explicitly in both rescheduleIfRecurring and the
    retry path). This mirrors Future.cancel(false) rather than Thread.interrupt() semantics -
    interrupting an arbitrary in-flight Callable safely is a much bigger commitment (the
    job would have to be interrupt-aware) that wasn't asked for here.

    Fixed-rate vs fixed-delay is a real semantic difference, not a naming choice:
    fixed-rate anchors the next run to the *previous scheduled time* (so a slow run
    doesn't push the whole series later - it may even cause back-to-back catch-up runs);
    fixed-delay anchors to the *previous completion time* (so the gap between runs is
    always constant, but the series as a whole drifts if runs are slow). That's why
    RecurrenceStrategy.nextExecutionTimeMillis takes both timestamps and each strategy
    picks the one it needs, rather than the caller having to know which strategy wants which.

    Known limitation, worth naming out loud in an interview: taskRegistry never evicts
    terminal (COMPLETED/FAILED/CANCELLED) one-time tasks, so it grows unbounded over the
    life of the process. Fine for a demo; a real service would TTL-evict terminal entries
    or require the caller to explicitly acknowledge/reap them.

Build & run
    javac -d out $(find src/taskscheduler -name "*.java")
    java -cp out taskscheduler.Simulation
