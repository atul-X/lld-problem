# Java Concurrency & Thread Safety
### A Small Book

*A working engineer's reference — from the memory model up to virtual threads, with the Spring Boot reality in between.*

---

## Table of Contents

**Part I — Foundations**
1. [The Three Problems](#1-the-three-problems)
2. [The Java Memory Model](#2-the-java-memory-model)
3. [Threads: Lifecycle and Mechanics](#3-threads-lifecycle-and-mechanics)

**Part II — Achieving Thread Safety**
4. [The Ladder of Strategies](#4-the-ladder-of-strategies)
5. [`synchronized` and Intrinsic Locks](#5-synchronized-and-intrinsic-locks)
6. [`volatile`: What It Does and Doesn't](#6-volatile-what-it-does-and-doesnt)
7. [Atomics and CAS](#7-atomics-and-cas)
8. [Explicit Locks and the Selection Guide](#8-explicit-locks-and-the-selection-guide)

**Part III — Coordination**
9. [Waiting: `wait`/`notify` and `Condition`](#9-waiting-waitnotify-and-condition)
10. [Synchronizers](#10-synchronizers)
11. [Concurrent Collections](#11-concurrent-collections)

**Part IV — Executing Work**
12. [Executors and Thread Pools](#12-executors-and-thread-pools)
13. [`CompletableFuture`](#13-completablefuture)
14. [Fork/Join and Parallel Streams](#14-forkjoin-and-parallel-streams)
15. [Virtual Threads and Structured Concurrency](#15-virtual-threads-and-structured-concurrency)

**Part V — In Production**
16. [Liveness Failures](#16-liveness-failures)
17. [Performance and Contention](#17-performance-and-contention)
18. [Testing and Debugging](#18-testing-and-debugging)
19. [Concurrency in a Spring Boot Service](#19-concurrency-in-a-spring-boot-service)
20. [The Interview Playbook](#20-the-interview-playbook)

**Appendices**
- [A. Decision Tables](#appendix-a-decision-tables)
- [B. Bug Gallery](#appendix-b-bug-gallery)
- [C. Cheat Sheet](#appendix-c-cheat-sheet)

---
---

# Part I — Foundations

## 1. The Three Problems

Almost every concurrency bug you will ever write is one of exactly three things. Learn to name them and debugging becomes classification instead of guesswork.

### 1.1 Atomicity

An operation is atomic if no other thread can observe it half-done.

```java
class Counter {
    private int count = 0;
    public void increment() { count++; }   // NOT atomic
}
```

`count++` is three bytecodes: read, add, write. Two threads can both read `7`, both write `8`, and one increment vanishes. This is a **lost update** — the same failure mode as two API calls debiting a wallet balance concurrently.

The unit of atomicity you care about is rarely a single operation. It's the **invariant**:

```java
// Two fields, one invariant: lower <= upper
class NumberRange {
    private final AtomicInteger lower = new AtomicInteger(0);
    private final AtomicInteger upper = new AtomicInteger(0);

    public void setLower(int i) {
        if (i > upper.get()) throw new IllegalArgumentException();
        lower.set(i);          // check-then-act across two atomics
    }                          // => still broken
}
```

Both fields are individually atomic. The class is still not thread-safe, because the invariant spans both. **Atomic parts do not compose into an atomic whole.** Every compound action — check-then-act, read-modify-write — needs a single point of mutual exclusion covering the whole invariant.

### 1.2 Visibility

There is no guarantee that a write by thread A is ever seen by thread B, absent synchronization.

```java
class StopFlag {
    private boolean stop = false;          // no volatile
    public void run() { while (!stop) { /* work */ } }
    public void stop() { stop = true; }
}
```

This loop can run forever. Not "usually works, sometimes doesn't" — the JIT is *permitted* to hoist `stop` into a register and compile the loop to `while (true)`. It regularly does exactly that after the loop gets hot.

Where the stale value physically lives — CPU store buffer, L1 cache, a register — is an implementation detail. The rule is simpler: **without a happens-before edge, one thread's writes are invisible to another, indefinitely.**

### 1.3 Ordering

The compiler, the JIT, and the CPU all reorder instructions. Within a single thread this is invisible ("as-if-serial"). Across threads, it's observable.

```java
// Thread A            // Thread B
x = 1;                 r1 = y;
y = 1;                 r2 = x;
```

`r1 == 1 && r2 == 0` is a legal outcome on x86 and common on ARM. Thread B saw the second write but not the first.

This is why **double-checked locking without `volatile` is broken**:

```java
class BrokenSingleton {
    private static Resource instance;      // needs volatile
    public static Resource get() {
        if (instance == null) {
            synchronized (BrokenSingleton.class) {
                if (instance == null) instance = new Resource();
            }
        }
        return instance;                   // may return a half-built object
    }
}
```

`instance = new Resource()` is: allocate memory, run the constructor, publish the reference. Steps 2 and 3 can be reordered. Another thread reading `instance` outside the lock can get a non-null reference to an object whose fields are still zeros. Marking the field `volatile` forbids the reorder.

> **The core insight.** These three problems are not independent bugs to be patched. They are the three things the Java Memory Model exists to let you control. Everything in Part II is a different-priced tool for buying atomicity, visibility, and ordering.

---

## 2. The Java Memory Model

The JMM is a contract. It does not say what the hardware does; it says what results a correctly-synchronized program is *allowed* to observe.

### 2.1 Happens-before

If action A *happens-before* action B, then A's effects are visible to B, and A appears to occur first. If there is no happens-before edge between two accesses and at least one is a write, you have a **data race** and the JMM promises nothing.

The edges you get for free:

| # | Rule | Edge |
|---|------|------|
| 1 | Program order | Within one thread, earlier statements happen-before later ones |
| 2 | Monitor lock | Unlock of monitor M happens-before every subsequent lock of M |
| 3 | Volatile | Write to a volatile field happens-before every subsequent read of it |
| 4 | Thread start | `t.start()` happens-before everything inside `t` |
| 5 | Thread join | Everything in `t` happens-before `t.join()` returning |
| 6 | Interruption | `t.interrupt()` happens-before the interrupt being detected |
| 7 | Finalizer | End of a constructor happens-before the object's finalizer |
| 8 | Transitivity | A hb B and B hb C ⟹ A hb C |

Rule 8 is what makes the model usable. You almost never reason about a single edge; you chain them.

```java
// Thread A
data = compute();       // 1: plain write
ready = true;           // 2: volatile write

// Thread B
if (ready) {            // 3: volatile read
    use(data);          // 4: sees the value from (1)
}
```

(1) hb (2) by program order. (2) hb (3) by the volatile rule. (3) hb (4) by program order. Therefore (1) hb (4). Thread B sees `data` correctly **even though `data` is not volatile.** A volatile write acts as a release fence for everything written before it. This is the single most useful pattern in the model — it's how `AtomicBoolean` flags, `ConcurrentHashMap`, and lock-based publication all work under the hood.

### 2.2 Safe publication

An object is *safely published* if the reference and its state become visible to other threads simultaneously. Four sanctioned ways:

1. Initialize the reference from a **static initializer** (the JVM guarantees class-init synchronization).
2. Store it into a **`volatile` field** or `AtomicReference`.
3. Store it into a **`final` field** of a properly constructed object.
4. Store it into a field **guarded by a lock**, and read it under the same lock.

Unsafe publication is the classic:

```java
public Holder holder;                       // plain field
public void init() { holder = new Holder(42); }  // race
```

Another thread can see `holder != null` and `holder.value == 0`.

### 2.3 Final field semantics

`final` fields carry a special guarantee: if an object is properly constructed, any thread that sees a reference to it sees its `final` fields fully initialized — **without synchronization.**

```java
class Point {
    final int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
}
```

Caveat: this holds only if `this` does not escape the constructor. The moment you register a listener, start a thread, or hand `this` to anything from inside the constructor, the guarantee is void.

```java
class Broken {
    final int value;
    Broken(EventBus bus) {
        bus.register(this);   // `this` escapes; another thread may see value == 0
        this.value = 42;
    }
}
```

Fix: use a static factory that constructs fully, then registers.

### 2.4 What "thread-safe" actually means

> A class is thread-safe if it behaves correctly when accessed from multiple threads, **regardless of scheduling or interleaving, with no additional synchronization on the caller's part.**

Two implications people miss:

- **Thread safety is a property of the class's invariants, not of its methods.** Adding `synchronized` to every method does not make a class thread-safe if callers need to compose operations atomically.
- **Stateless classes are always thread-safe.** A Spring `@Service` with no mutable fields, only injected singleton collaborators, is thread-safe by construction. This is why the standard Spring layering works at all under concurrent load.

---

## 3. Threads: Lifecycle and Mechanics

### 3.1 States

```
        new Thread()
             |
          [NEW]
             | start()
             v
       [RUNNABLE] <-----------------------+
        |   |   |                         |
        |   |   | wait()/join()/park()    | notify()/join returns/unpark()
        |   |   +---> [WAITING] ----------+
        |   |                             |
        |   | wait(ms)/sleep(ms)          |
        |   +---> [TIMED_WAITING] --------+
        |                                 |
        | contended synchronized entry    |
        +---> [BLOCKED] ------------------+ (lock acquired)
             |
             | run() returns / throws
             v
       [TERMINATED]
```

Two distinctions that come up in interviews and in thread dumps:

- **`BLOCKED` vs `WAITING`.** `BLOCKED` means: waiting to *acquire* a monitor. Nobody signalled anything; the thread just lost a race. `WAITING` means: the thread voluntarily gave up the CPU and needs another thread to wake it. In a thread dump, a wall of `BLOCKED` threads on one monitor is a contention hotspot; a wall of `WAITING` threads is usually a pool that's idle or a coordination bug.
- **`RUNNABLE` does not mean running.** A thread blocked on a socket read shows as `RUNNABLE` because the JVM can't see OS-level I/O blocking. This misleads people reading dumps: `RUNNABLE` + a stack ending in `socketRead0` = blocked on I/O, not burning CPU.

### 3.2 Interruption is a request, not a kill

`Thread.stop()` is deprecated and dangerous (it throws asynchronously at an arbitrary point, leaving invariants broken). The cooperative mechanism is interruption:

```java
public void run() {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            Object item = queue.take();      // throws InterruptedException
            process(item);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // restore the flag
    } finally {
        cleanup();
    }
}
```

**The rule that matters:** when a blocking method throws `InterruptedException`, it *clears* the interrupt flag. If you catch it and don't rethrow, you must restore the flag with `Thread.currentThread().interrupt()`. Otherwise the interruption is silently swallowed and code higher in the stack — an executor trying to shut down, for instance — never learns it should stop.

Never do this:

```java
catch (InterruptedException e) { }                    // swallowed
catch (InterruptedException e) { log.error("", e); }  // also swallowed
```

Only code that owns the thread (i.e. the top of a `Runnable`) may decide to absorb an interrupt. Library and utility code must propagate.

### 3.3 Daemon threads

A daemon thread does not prevent JVM exit. Set before `start()`. Executor pools create non-daemon threads by default, which is why a Spring Boot app that forgot to shut down a pool hangs on SIGTERM instead of exiting. Prefer explicit lifecycle management over daemon flags — daemon threads are killed abruptly at exit with no `finally` blocks run.

### 3.4 ThreadLocal

Per-thread storage. Legitimate uses: request-scoped context (MDC for logging, security principal, tenant ID), and reusing non-thread-safe objects like `SimpleDateFormat`.

Two failure modes to keep in mind:

1. **Leaks in pooled threads.** A pool thread lives forever; whatever you put in a `ThreadLocal` lives with it, keeping a classloader or a large object graph alive. Always `remove()` in a `finally`.
2. **It does not propagate.** Hand work to an executor and the `ThreadLocal` is gone. This is why MDC logging context vanishes inside `@Async` methods. Fixes: `InheritableThreadLocal` (only covers thread *creation*, not pool reuse), a `TaskDecorator` that copies context, or Micrometer's `ContextPropagation`.

```java
// Spring: propagate MDC into an @Async pool
public class MdcTaskDecorator implements TaskDecorator {
    @Override public Runnable decorate(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (context != null) MDC.setContextMap(context);
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

Java 21 adds `ScopedValue` — immutable, explicitly scoped, and inherited by structured-concurrency forks. It's the right replacement where it fits.

---
---

# Part II — Achieving Thread Safety

## 4. The Ladder of Strategies

Before reaching for a lock, walk down this ladder. Each rung is cheaper, simpler, and harder to get wrong than the one below it.

| Rung | Strategy | Cost | When it applies |
|------|----------|------|-----------------|
| 1 | **Don't share state** | Free | Stateless services, local variables, per-request objects |
| 2 | **Immutability** | Free after construction | Value objects, config, DTOs, event payloads |
| 3 | **Thread confinement** | Free | ThreadLocal, single-writer designs, event loops |
| 4 | **Delegate to a thread-safe class** | Cheap | `ConcurrentHashMap`, `AtomicLong`, `BlockingQueue` |
| 5 | **Atomics / CAS** | Low, scales under contention... to a point | Counters, flags, simple state machines |
| 6 | **Locks** | Moderate; serializes | Multi-field invariants, compound actions |
| 7 | **Distributed coordination** | Expensive, failure-prone | State shared across JVMs |

The single highest-leverage habit in concurrent programming: **spend your effort moving up this ladder, not perfecting rung 6.**

### 4.1 Immutability

An object is immutable if all fields are `final`, the state is fully built in the constructor, `this` doesn't escape, and any mutable components are defensively copied on the way in and out.

```java
public final class TransactionRequest {
    private final String idempotencyKey;
    private final BigDecimal amount;
    private final Map<String, String> metadata;

    public TransactionRequest(String key, BigDecimal amount, Map<String, String> meta) {
        this.idempotencyKey = key;
        this.amount = amount;
        this.metadata = Map.copyOf(meta);        // defensive copy, unmodifiable
    }
    public Map<String, String> getMetadata() { return metadata; }
}
```

Java `record`s give you most of this for free — but note that a record holding a `List` field is **not** immutable unless you copy in the compact constructor:

```java
public record Order(String id, List<Item> items) {
    public Order {
        items = List.copyOf(items);   // otherwise the caller keeps a mutable handle
    }
}
```

`BigDecimal`, `String`, `LocalDate`, `Instant` are immutable. `Date`, `Calendar`, `SimpleDateFormat` are not — the last one is a perennial production bug when stored in a static field.

### 4.2 Volatile-immutable pattern

When you need mutable state but the invariant spans multiple fields, bundle them into an immutable holder and swap the reference atomically:

```java
class RateLimitConfig {
    private static final class Snapshot {
        final int permits; final Duration window;
        Snapshot(int p, Duration w) { permits = p; window = w; }
    }
    private volatile Snapshot current = new Snapshot(100, Duration.ofSeconds(1));

    public void update(int permits, Duration window) {
        current = new Snapshot(permits, window);   // atomic swap, no lock
    }
    public int permits() { return current.permits; }
}
```

Readers are lock-free and always see a *consistent pair*. This is the right shape for hot-reloaded configuration.

---

## 5. `synchronized` and Intrinsic Locks

Every Java object has an intrinsic lock (monitor). `synchronized` acquires it on entry and releases on exit — including on exception, which is a real advantage over `ReentrantLock`.

```java
synchronized void m() { ... }              // locks `this`
static synchronized void s() { ... }       // locks TheClass.class
void m() { synchronized (lockObject) { ... } }   // locks a chosen object
```

### 5.1 Properties

- **Mutual exclusion + memory barrier.** Entering gives you visibility of everything the previous holder did before releasing.
- **Reentrant.** A thread can re-acquire a lock it already holds. Without this, a synchronized method calling another synchronized method on the same object would self-deadlock.
- **Not interruptible, no timeout, no fairness control.** A thread `BLOCKED` on a monitor stays blocked.

### 5.2 Lock the right object

Locking `this` publishes your lock to the world — any caller can `synchronized (yourObject)` and interfere. Prefer a private lock:

```java
public class Ledger {
    private final Object lock = new Object();
    private long balance;
    public void transfer(long amount) {
        synchronized (lock) { balance += amount; }
    }
}
```

Never lock on:
- **`String` literals** — interned and shared JVM-wide.
- **Boxed primitives** (`Integer`, `Boolean`) — cached in the range −128..127, so `synchronized(count)` where `count` is an `Integer` may share a lock with unrelated code.
- **A field you reassign** — you'd be locking different objects on different calls.

### 5.3 Keep the block small — but not too small

```java
// Too coarse: holds the lock across a network call
public synchronized void process(Order o) {
    validate(o);
    paymentGateway.charge(o);     // 800ms with the lock held
    cache.put(o.id(), o);
}

// Right: lock only the shared-state mutation
public void process(Order o) {
    validate(o);
    ChargeResult r = paymentGateway.charge(o);
    synchronized (lock) { cache.put(o.id(), o); }
}
```

**Never hold a lock across I/O.** A 200ms lock held under 500 rps doesn't queue — it collapses the service. But don't split one logical invariant into several small blocks either; that reintroduces the atomicity bug you were trying to prevent.

### 5.4 Client-side locking and composition

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

// BROKEN: each call is atomic, the pair is not
if (!list.contains(x)) list.add(x);

// Works, because synchronizedList locks on the wrapper itself
synchronized (list) {
    if (!list.contains(x)) list.add(x);
}
```

This works only because that's a documented implementation detail. Depending on it is fragile. Better: **composition** — wrap the collection in your own class with your own lock, and expose only atomic operations. Better still: use `ConcurrentHashMap.putIfAbsent` and stop hand-rolling.

---

## 6. `volatile`: What It Does and Doesn't

`volatile` gives you **visibility and ordering. It does not give you atomicity.**

```java
private volatile int count;
public void increment() { count++; }   // STILL BROKEN
```

Read-modify-write is not atomic no matter how volatile the field is.

### 6.1 The legitimate uses

**1. Status / stop flags**

```java
private volatile boolean running = true;
public void shutdown() { running = false; }
public void run() { while (running) { poll(); } }
```

**2. One-time safe publication**

```java
private volatile Config config;   // written once by a loader, read by many
```

**3. Double-checked locking**

```java
public class LazyHolder {
    private volatile Resource resource;
    public Resource get() {
        Resource r = resource;
        if (r == null) {
            synchronized (this) {
                r = resource;
                if (r == null) resource = r = new Resource();
            }
        }
        return r;
    }
}
```

(The local variable `r` avoids repeated volatile reads — a real, measurable micro-optimization in hot paths.)

For a lazily-initialized *static*, prefer the initialization-on-demand holder idiom — no volatile, no lock, and the JVM does the work:

```java
public class Singleton {
    private Singleton() {}
    private static class Holder { static final Singleton INSTANCE = new Singleton(); }
    public static Singleton get() { return Holder.INSTANCE; }
}
```

**4. Independent observation** — a field written by one thread and read by others where no invariant links it to anything else (e.g. a `lastUpdatedTimestamp` for monitoring).

### 6.2 The test

Use `volatile` only when **all** of these hold:
- Writes do not depend on the current value (or you have a single writer), **and**
- The variable participates in no invariant with other state, **and**
- No lock is required for any other reason while accessing it.

If any fails, you need an atomic or a lock.

---

## 7. Atomics and CAS

### 7.1 Compare-and-swap

CAS is a hardware instruction (`lock cmpxchg` on x86): *"if this memory location still holds the expected value, write the new value; report whether you succeeded."* Atomically, in one instruction.

```java
// What incrementAndGet() essentially does
int prev, next;
do {
    prev = get();
    next = prev + 1;
} while (!compareAndSet(prev, next));
```

This is **optimistic**: instead of preventing other threads from interfering, you detect interference and retry. No blocking, no context switch, no deadlock possible.

The tradeoff: under heavy contention the retry loop burns CPU. A lock parks the thread and lets the CPU do something else; CAS spins. Around 8–16 contending threads, a well-implemented lock often overtakes naive CAS.

### 7.2 The atomic family

| Class | Use |
|-------|-----|
| `AtomicInteger`, `AtomicLong` | Counters, sequence numbers, IDs |
| `AtomicBoolean` | One-shot flags (`compareAndSet(false, true)` = "I won the race") |
| `AtomicReference<T>` | Lock-free state machines, immutable-snapshot swaps |
| `AtomicIntegerArray` etc. | Element-wise atomic array slots |
| `LongAdder`, `DoubleAdder` | **High-contention counters** |
| `AtomicStampedReference` | ABA-safe reference updates |
| `AtomicIntegerFieldUpdater` | Atomic ops on a `volatile` field without a wrapper object (memory-footprint optimization) |

### 7.3 `LongAdder` — the one people miss

`AtomicLong` under contention has every thread CASing the *same* cache line. `LongAdder` keeps an array of cells, striped so different threads hit different cells, and sums them on `sum()`.

```java
// Metrics counter hit thousands of times per second
private final LongAdder requestCount = new LongAdder();
public void onRequest() { requestCount.increment(); }
public long total() { return requestCount.sum(); }   // approximate if concurrent writes
```

**Rule:** if you write far more often than you read, and you only need the total — use `LongAdder`. It can be an order of magnitude faster. If you need `getAndIncrement()` to return an accurate per-call value (sequence numbers, IDs), you need `AtomicLong`.

### 7.4 Non-trivial atomic updates

```java
AtomicReference<Balance> ref = new AtomicReference<>(new Balance(0, 0));

// Lambda may be invoked multiple times — it MUST be pure and side-effect free
ref.updateAndGet(b -> new Balance(b.available() - amt, b.reserved() + amt));

map.compute(key, (k, v) -> v == null ? 1 : v + 1);   // same rule applies
```

The single most common bug here is putting a side effect (a log line, a DB call, a counter increment) inside the update function. Under contention it runs twice.

### 7.5 The ABA problem

Thread A reads value `A`. Thread B changes it to `B` and back to `A`. Thread A's CAS succeeds — but the world changed underneath it. Harmless for counters; fatal for lock-free stacks and linked structures, where the node may have been recycled.

```java
AtomicStampedReference<Node> head = new AtomicStampedReference<>(null, 0);
int[] stamp = new int[1];
Node current = head.get(stamp);
head.compareAndSet(current, newNode, stamp[0], stamp[0] + 1);  // version guards it
```

---

## 8. Explicit Locks and the Selection Guide

### 8.1 `ReentrantLock`

Everything `synchronized` does, plus: timeouts, interruptibility, fairness, multiple condition queues, and non-block-structured locking.

```java
private final ReentrantLock lock = new ReentrantLock();

public void transfer() {
    lock.lock();
    try {
        // critical section
    } finally {
        lock.unlock();          // MANDATORY — not automatic
    }
}
```

The `finally` is not optional. A missing `unlock()` on an exception path is a permanent, unrecoverable deadlock — the one real downside versus `synchronized`.

**`tryLock` — the deadlock escape hatch:**

```java
if (lock.tryLock(200, TimeUnit.MILLISECONDS)) {
    try { doWork(); } finally { lock.unlock(); }
} else {
    metrics.increment("lock.timeout");
    throw new ResourceBusyException();   // fail fast instead of piling up threads
}
```

Under load, failing fast is almost always better than an unbounded queue of blocked threads. This is the lock-level equivalent of a circuit breaker.

**Fairness:** `new ReentrantLock(true)` grants the lock in FIFO order. It eliminates starvation and costs roughly an order of magnitude in throughput, because it forbids barging (a running thread taking a just-released lock instead of the JVM waking a parked one). Default to unfair. Choose fair only when starvation is demonstrably real and lock hold times are long.

### 8.2 `ReentrantReadWriteLock`

Many concurrent readers, or one exclusive writer.

```java
private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

public V get(K key) {
    rw.readLock().lock();
    try { return map.get(key); } finally { rw.readLock().unlock(); }
}
public void put(K key, V value) {
    rw.writeLock().lock();
    try { map.put(key, value); } finally { rw.writeLock().unlock(); }
}
```

Worth it only when reads **greatly** outnumber writes *and* the critical section is long enough to amortize the bookkeeping. For a short-critical-section map, `ConcurrentHashMap` beats this comfortably.

Lock downgrading is legal; upgrading is not (and deadlocks if you try):

```java
rw.writeLock().lock();
try {
    cache = recompute();
    rw.readLock().lock();          // acquire read BEFORE releasing write
} finally { rw.writeLock().unlock(); }
try { use(cache); } finally { rw.readLock().unlock(); }
```

### 8.3 `StampedLock` (Java 8+)

Adds an **optimistic read** mode: read without acquiring anything, then validate that no write intervened.

```java
private final StampedLock sl = new StampedLock();
private double x, y;

double distanceFromOrigin() {
    long stamp = sl.tryOptimisticRead();     // no lock acquired
    double cx = x, cy = y;
    if (!sl.validate(stamp)) {               // a writer got in — fall back
        stamp = sl.readLock();
        try { cx = x; cy = y; } finally { sl.unlockRead(stamp); }
    }
    return Math.sqrt(cx * cx + cy * cy);
}
```

Fast when writes are rare. But: **not reentrant** (re-entering self-deadlocks), stamps must be managed by hand, and it doesn't support `Condition`. Use it in a small, carefully reviewed hot spot, not as a general-purpose lock.

### 8.4 Lock selection guide

| Situation | Use |
|-----------|-----|
| Simple mutual exclusion, short section | `synchronized` |
| Need timeout, interruptibility, or `tryLock` | `ReentrantLock` |
| Need multiple wait conditions (e.g. notFull / notEmpty) | `ReentrantLock` + `Condition` |
| Starvation is a proven problem | `ReentrantLock(true)` |
| Read-heavy, long critical sections | `ReentrantReadWriteLock` |
| Read-heavy, very short sections, writes rare, non-reentrant is fine | `StampedLock` |
| Single variable, moderate contention | `AtomicX` |
| Single counter, extreme write contention | `LongAdder` |
| Map/queue/list access | The matching concurrent collection — no lock at all |
| State shared across JVMs | Distributed lock (Redis/ZooKeeper) — see §19.6 |

**Default to `synchronized`.** It's the simplest correct thing, the JIT optimizes it aggressively (biased locking history aside, it handles uncontended cases nearly for free), and it can't leak. Escalate only with a reason you can articulate.

---
---

# Part III — Coordination

## 9. Waiting: `wait`/`notify` and `Condition`

### 9.1 The intrinsic condition queue

`wait()`, `notify()`, and `notifyAll()` are methods on `Object` and may only be called while holding that object's monitor. `wait()` atomically releases the lock and parks the thread; on wake-up it re-acquires the lock before returning.

**The canonical form — memorize this shape:**

```java
synchronized (lock) {
    while (!conditionHolds()) {      // WHILE, never IF
        lock.wait();
    }
    // condition holds and we hold the lock
    doWork();
}
```

Why `while` and not `if`:
- **Spurious wakeups** are permitted by the spec.
- **`notifyAll` wakes everyone**, but only one can proceed; the others must re-check.
- **Barging:** between the notify and the waiter re-acquiring the lock, a third thread can grab the lock and invalidate the condition again.

`notify()` wakes one arbitrary waiter; `notifyAll()` wakes all. `notify()` is an optimization that is **only safe when every waiter is waiting for the same condition and any one of them can make progress.** If waiters are heterogeneous, `notify()` can wake the wrong one and stall the system permanently. Default to `notifyAll()` unless you can prove uniformity.

### 9.2 `Condition` — multiple wait sets per lock

The explicit-lock version, and strictly more capable: one lock can own several condition queues.

```java
public class BoundedBuffer<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Object[] items;
    private int head, tail, count;

    public BoundedBuffer(int capacity) { items = new Object[capacity]; }

    public void put(T x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) notFull.await();
            items[tail] = x;
            if (++tail == items.length) tail = 0;
            count++;
            notEmpty.signal();          // safe: all waiters here are consumers
        } finally { lock.unlock(); }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) notEmpty.await();
            T x = (T) items[head];
            items[head] = null;
            if (++head == items.length) head = 0;
            count--;
            notFull.signal();
            return x;
        } finally { lock.unlock(); }
    }
}
```

Two condition queues let you use `signal()` instead of `signalAll()` safely — producers and consumers never wake each other pointlessly. This is exactly why `ArrayBlockingQueue` is built this way, and it's the classic "implement a bounded buffer" interview answer.

Note `await()` vs `wait()` — mixing them up (calling `wait()` on a `Condition` object) is a compile-time-legal, runtime-broken mistake.

`awaitUninterruptibly()`, `await(timeout, unit)`, and `awaitNanos()` cover the variants.

---

## 10. Synchronizers

All of these are built on `AbstractQueuedSynchronizer` (AQS) — an `int` state field plus a CLH queue of parked threads. Knowing that AQS underpins `ReentrantLock`, `Semaphore`, `CountDownLatch`, and `ReentrantReadWriteLock` is a common interview follow-up.

### `CountDownLatch` — one-shot gate

```java
CountDownLatch ready = new CountDownLatch(services.size());
for (Service s : services) {
    executor.submit(() -> { try { s.warmUp(); } finally { ready.countDown(); } });
}
if (!ready.await(30, TimeUnit.SECONDS)) throw new StartupTimeoutException();
```

Cannot be reset. `countDown()` in a `finally`, always — otherwise a task that throws hangs the latch forever. Always use the timed `await`.

### `CyclicBarrier` — reusable rendezvous

All N threads wait for each other, then all proceed; the barrier resets. Optional barrier action runs once, on the last-arriving thread.

```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> mergePartialResults());
```

If any participant leaves or times out, the barrier is **broken** and all others get `BrokenBarrierException`. Use for iterative parallel algorithms (simulation steps, ML epochs), not for general task coordination.

### `Semaphore` — permit-based throttle

```java
private final Semaphore permits = new Semaphore(10);   // max 10 concurrent

public Response call() throws InterruptedException {
    if (!permits.tryAcquire(500, TimeUnit.MILLISECONDS)) {
        throw new BulkheadFullException();
    }
    try { return downstream.invoke(); } finally { permits.release(); }
}
```

This is the **bulkhead pattern** — capping concurrency to a fragile downstream so it can't drag your whole thread pool down. `release()` in `finally`; a leaked permit is a slow-motion outage. A `Semaphore(1)` is a non-reentrant mutex that can be released by a different thread than acquired it — occasionally exactly what you need.

### `Phaser` — dynamic barrier

`CyclicBarrier` with a registration count that can change at runtime, plus multi-phase support. Powerful, rarely needed. Reach for it when parties join and leave between phases.

### `Exchanger` — two-party swap

Pairs of threads swap objects at a rendezvous point. Niche: double-buffering pipelines.

---

## 11. Concurrent Collections

### 11.1 The landscape

| Need | Use | Avoid |
|------|-----|-------|
| Map | `ConcurrentHashMap` | `Hashtable`, `synchronizedMap` |
| Sorted map | `ConcurrentSkipListMap` | `synchronizedSortedMap` |
| Set | `ConcurrentHashMap.newKeySet()` | `synchronizedSet` |
| List, read-dominated | `CopyOnWriteArrayList` | `synchronizedList` |
| Producer/consumer queue | `ArrayBlockingQueue` / `LinkedBlockingQueue` | hand-rolled `wait/notify` |
| Unbounded non-blocking queue | `ConcurrentLinkedQueue` | — |
| Priority + blocking | `PriorityBlockingQueue` | — |
| Scheduled/delayed items | `DelayQueue` | — |
| Work stealing | `ConcurrentLinkedDeque` | — |

The `Collections.synchronizedXxx` wrappers wrap every method in one global lock. They serialize all access and still don't make compound operations atomic. They exist for legacy compatibility; treat them as deprecated in new code.

### 11.2 `ConcurrentHashMap`

**Java 7:** segmented — 16 independent locks, concurrency limited by segment count.
**Java 8+:** one table, **per-bin locking**. Reads are entirely lock-free (`volatile` node values plus `Unsafe.getObjectVolatile` on the table). Writes CAS an empty bin, or `synchronized` on the bin's head node if occupied. Bins with ≥8 entries convert from linked list to red-black tree, bounding worst-case lookup at O(log n) — this is also the hash-collision-DoS mitigation.

**Atomic compound operations — use these instead of check-then-act:**

```java
map.putIfAbsent(key, value);                          // insert if absent
map.computeIfAbsent(key, k -> expensiveLoad(k));      // lazy cache fill
map.compute(key, (k, v) -> v == null ? 1 : v + 1);    // atomic counter per key
map.merge(key, 1L, Long::sum);                        // idiomatic counting
map.replace(key, oldVal, newVal);                     // CAS semantics
map.remove(key, expectedValue);                       // conditional remove
```

Three sharp edges:

1. **The remapping function runs under the bin lock.** Never do I/O, never call another map operation on the same map, never block inside it. A `computeIfAbsent` whose lambda touches the same map deadlocks (Java 9+ detects some cases and throws instead of hanging).
2. **No `null` keys or values.** Deliberate: `get()` returning null would be ambiguous between "absent" and "mapped to null" in a map you can't atomically inspect. Use a sentinel or `Optional` as the value.
3. **Iterators are weakly consistent.** They never throw `ConcurrentModificationException`, reflect state at some point during traversal, and may or may not show concurrent updates. `size()` and `isEmpty()` are approximations. Don't build logic on them.

### 11.3 `CopyOnWriteArrayList`

Every mutation copies the whole backing array. Reads are completely free — no locking, no volatile read overhead beyond the array reference. Iterators snapshot at creation and never throw CME.

Perfect for listener/observer registries: written once at startup, iterated on every event. Catastrophic for anything write-heavy — an O(n) copy per write.

### 11.4 Blocking queues

The backbone of producer-consumer and the handoff mechanism inside every `ThreadPoolExecutor`.

| Method | Blocks | Returns special | Throws | Times out |
|--------|--------|-----------------|--------|-----------|
| Insert | `put(e)` | `offer(e)` → false | `add(e)` | `offer(e, t, u)` |
| Remove | `take()` | `poll()` → null | `remove()` | `poll(t, u)` |
| Examine | — | `peek()` → null | `element()` | — |

```java
// Bounded queue = built-in backpressure. This is the point.
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(1000);

// Producer: blocks when full instead of exhausting the heap
queue.put(task);

// Consumer with poison-pill shutdown
while (true) {
    Task t = queue.take();
    if (t == POISON_PILL) break;
    process(t);
}
```

**Always bound your queues.** An unbounded `LinkedBlockingQueue` in front of a slow consumer is an `OutOfMemoryError` waiting for enough traffic. The bound is what converts "silently accumulate until death" into "apply backpressure or reject."

- `ArrayBlockingQueue` — bounded, single lock, optional fairness. Predictable memory.
- `LinkedBlockingQueue` — optionally bounded, separate head/tail locks → higher throughput under simultaneous producer and consumer load.
- `SynchronousQueue` — zero capacity; every `put` waits for a matching `take`. Direct handoff. This is what `newCachedThreadPool` uses.
- `LinkedTransferQueue` — `transfer()` blocks until a consumer actually receives the element. The most performant general-purpose unbounded queue.
- `DelayQueue` — elements become available only after their delay expires. Natural fit for retry scheduling and expiring caches.
- `PriorityBlockingQueue` — unbounded, ordered by comparator. Note: unbounded, so no backpressure.

---
---

# Part IV — Executing Work

## 12. Executors and Thread Pools

Creating a `Thread` per task fails at scale: each thread costs ~1MB of stack, creation is expensive, and there's no bound on how many you get. The Executor framework separates **task submission** from **execution policy**.

### 12.1 The core abstraction

```java
public interface Executor { void execute(Runnable command); }
```

`ExecutorService` adds lifecycle (`shutdown`, `awaitTermination`) and `Future`-returning `submit`. `ScheduledExecutorService` adds delayed and periodic execution.

### 12.2 `ThreadPoolExecutor`: the real constructor

The `Executors.newXxx` factories hide the parameters that matter. Learn the full constructor:

```java
new ThreadPoolExecutor(
    corePoolSize,        // threads kept alive even when idle
    maximumPoolSize,     // hard ceiling
    keepAliveTime, unit, // idle timeout for threads above core
    workQueue,           // where tasks wait
    threadFactory,       // naming, daemon status, uncaught handler
    rejectedExecutionHandler
);
```

**The task-arrival algorithm — this is the part people get wrong:**

1. Fewer than `corePoolSize` threads? Create a new thread. (Even if others are idle.)
2. Otherwise, try to **enqueue**.
3. Queue full? Create a new thread, up to `maximumPoolSize`.
4. Queue full **and** at max threads? **Reject.**

The consequence: **with an unbounded queue, `maximumPoolSize` is never reached.** Step 2 always succeeds, so the pool never grows past core. This silently makes `newFixedThreadPool` semantics out of what you thought was an elastic pool.

### 12.3 Why the factory methods are traps

| Factory | Hidden behaviour | Risk |
|---------|------------------|------|
| `newFixedThreadPool(n)` | Unbounded `LinkedBlockingQueue` | Queue grows without limit → OOM |
| `newSingleThreadExecutor()` | Same | Same |
| `newCachedThreadPool()` | `SynchronousQueue`, max = `Integer.MAX_VALUE` | Unbounded thread creation → OOM / thrashing |
| `newScheduledThreadPool(n)` | Unbounded delayed queue | Same |

Build pools explicitly:

```java
@Bean("paymentExecutor")
public ThreadPoolExecutor paymentExecutor(MeterRegistry registry) {
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
        16, 32,
        60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(500),                     // BOUNDED
        new ThreadFactoryBuilder()
            .setNameFormat("payment-worker-%d")            // name your threads
            .setUncaughtExceptionHandler((t, e) -> log.error("uncaught in {}", t, e))
            .build(),
        new ThreadPoolExecutor.CallerRunsPolicy()          // backpressure
    );
    // instrument it — queue depth is your earliest saturation signal
    Gauge.builder("pool.queue.depth", pool, p -> p.getQueue().size()).register(registry);
    Gauge.builder("pool.active", pool, ThreadPoolExecutor::getActiveCount).register(registry);
    return pool;
}
```

Naming threads is not cosmetic. It's the difference between a readable thread dump and forty lines of `pool-3-thread-17`.

### 12.4 Rejection policies

| Policy | Behaviour | Use when |
|--------|-----------|----------|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` | You want to fail fast and surface it |
| `CallerRunsPolicy` | Caller thread runs the task | You want natural backpressure — the submitter slows down |
| `DiscardPolicy` | Silently drops | Best-effort telemetry only |
| `DiscardOldestPolicy` | Drops the head of the queue | Latest-value-wins feeds |

`CallerRunsPolicy` is the quietly excellent default for internal pipelines: when the pool saturates, the producing thread is conscripted into doing the work, which throttles ingestion at the source. Be careful using it on a request-handling thread — you're now doing background work on a latency-sensitive path.

### 12.5 Sizing

The starting point:

```
threads ≈ cores × targetUtilization × (1 + waitTime/computeTime)
```

- **CPU-bound:** `cores` or `cores + 1`. More threads only add context switches.
- **I/O-bound:** the ratio dominates. A task that spends 90ms waiting and 10ms computing on 8 cores suggests ~80 threads. In practice, cap by the *downstream* limit — your DB connection pool, or the rate limit on the API you're calling. There is no point having 200 threads contending for 20 DB connections; you've just moved the queue.
- **Then measure.** The formula gets you a starting number; queue depth, p99 latency, and CPU utilization under load get you the real one.

**Use separate pools for different workloads.** One shared pool means a slow downstream call starves your fast local computations. Distinct pools per dependency = bulkheading at the pool level.

### 12.6 Shutdown

```java
pool.shutdown();                                    // no new tasks; finish queued
if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
    List<Runnable> dropped = pool.shutdownNow();    // interrupt running tasks
    log.warn("Forced shutdown, {} tasks dropped", dropped.size());
    pool.awaitTermination(10, TimeUnit.SECONDS);
}
```

`shutdownNow()` interrupts running threads — which only works if your tasks respect interruption (§3.2). In Spring, a `@Bean` with `destroyMethod` or a `@PreDestroy` hook wires this into the container lifecycle. Skipping it is why a pod takes the full termination grace period to die.

### 12.7 The swallowed-exception trap

```java
executor.submit(() -> { throw new RuntimeException("boom"); });
// Nothing is logged. Nothing happens. The exception lives in the Future.
```

`submit()` captures the exception in the `Future`; if nobody calls `get()`, it disappears. `execute()` propagates to the thread's `UncaughtExceptionHandler`. This is a top-three source of "the job just stops working and there's nothing in the logs."

Defences: always set an `UncaughtExceptionHandler` on the `ThreadFactory`; wrap task bodies in try/catch; or override `afterExecute` to inspect both paths.

**Special case: a periodic task that throws is cancelled forever.** `scheduleAtFixedRate` silently stops rescheduling after the first uncaught exception. Wrap the body:

```java
scheduler.scheduleAtFixedRate(() -> {
    try { reconcile(); }
    catch (Exception e) { log.error("reconciliation failed", e); }   // swallow to survive
}, 0, 5, TimeUnit.MINUTES);
```

---

## 13. `CompletableFuture`

`Future` is nearly useless on its own: `get()` blocks, there's no composition, no callback, no error pipeline. `CompletableFuture` fixes all of it.

### 13.1 The method families

| Suffix pattern | Meaning |
|----------------|---------|
| `thenApply(fn)` | Transform the value → `CF<U>` |
| `thenCompose(fn)` | Chain another async call → flattens `CF<CF<U>>` |
| `thenCombine(other, fn)` | Join two independent futures |
| `thenAccept(c)` / `thenRun(r)` | Consume / side-effect, no value out |
| `...Async` | Run on the common pool (or a supplied executor) |
| `exceptionally(fn)` | Recover from failure |
| `handle(bifn)` | Handle both outcomes |
| `whenComplete(bifn)` | Observe both, pass through unchanged |

`thenApply` vs `thenCompose` is exactly `map` vs `flatMap`. If your function returns a `CompletableFuture`, use `thenCompose`.

### 13.2 A realistic composition

```java
public CompletableFuture<Dashboard> loadDashboard(String userId) {
    CompletableFuture<User> user =
        supplyAsync(() -> userService.find(userId), ioPool);

    CompletableFuture<List<Txn>> txns =
        supplyAsync(() -> txnService.recent(userId), ioPool)
            .orTimeout(2, TimeUnit.SECONDS)                     // Java 9+
            .exceptionally(e -> List.of());                     // degrade gracefully

    CompletableFuture<Score> score =
        supplyAsync(() -> scoreService.fetch(userId), ioPool)
            .completeOnTimeout(Score.unavailable(), 1, TimeUnit.SECONDS);

    return user.thenCombine(txns, Dashboard::new)
               .thenCombine(score, Dashboard::withScore)
               .whenComplete((d, e) -> {
                   if (e != null) log.error("dashboard load failed for {}", userId, e);
               });
}
```

Three independent calls run concurrently; partial failures degrade instead of failing the page. This is the fan-out/fan-in shape behind most BFF and aggregator endpoints.

### 13.3 The rules that bite

**1. Always pass your own executor.** The no-arg `Async` variants use `ForkJoinPool.commonPool()`, which is sized `cores - 1` and shared with parallel streams across the entire JVM. One blocking call there stalls unrelated code. On a 2-core container, `commonPool()` has **one** thread.

**2. `allOf` returns `CompletableFuture<Void>`.** Collect results afterwards:

```java
List<CompletableFuture<Result>> futures = ids.stream()
    .map(id -> supplyAsync(() -> fetch(id), pool))
    .toList();

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
```

Note the `.toList()` before joining — if you build the futures lazily inside a single stream pipeline, they execute sequentially and you get no concurrency at all. A very common silent bug.

**3. Exceptions get wrapped.** `join()` throws `CompletionException`; `get()` throws `ExecutionException`. Unwrap with `getCause()` before matching on type.

**4. `anyOf` completes on the first *outcome*, including the first failure.** For "first success wins," you need explicit handling.

**5. Callback thread is unspecified.** A non-`Async` continuation runs either on the completing thread or the caller's, whichever gets there. Never assume.

---

## 14. Fork/Join and Parallel Streams

Fork/Join targets **divide-and-conquer over in-memory data**, using work-stealing: each worker has a deque; idle workers steal from the *tail* of others' deques, which minimizes contention and preserves locality.

```java
class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10_000;
    private final long[] arr; private final int lo, hi;

    protected Long compute() {
        if (hi - lo <= THRESHOLD) {           // sequential base case
            long s = 0;
            for (int i = lo; i < hi; i++) s += arr[i];
            return s;
        }
        int mid = (lo + hi) >>> 1;
        SumTask left = new SumTask(arr, lo, mid);
        left.fork();                          // async
        long right = new SumTask(arr, mid, hi).compute();   // this thread
        return right + left.join();
    }
}
```

The idiom: fork one half, compute the other **on the current thread**, then join. Forking both wastes a thread. Getting the threshold right matters more than anything else — too small and coordination dominates.

### Parallel streams

```java
list.parallelStream().filter(...).map(...).collect(...);
```

Convenient, and the wrong tool more often than not.

**Use only when all hold:**
- Large N (rule of thumb: ≥10,000 elements) and genuinely CPU-bound work per element.
- A splittable source: arrays, `ArrayList`, `IntStream.range`. `LinkedList`, `Iterator`-based and I/O-backed streams split terribly.
- Operations are stateless, associative, side-effect free.
- You are not inside a request thread in a shared JVM.

**Never** put a blocking I/O call inside a parallel stream. It occupies `commonPool()` threads — which are shared process-wide — and you will stall everything else in the application, including other people's parallel streams. If you must, use a dedicated pool:

```java
ForkJoinPool pool = new ForkJoinPool(8);
pool.submit(() -> list.parallelStream().map(this::work).toList()).get();
```

Measure. Parallel streams frequently lose to the sequential version on realistic data sizes.

---

## 15. Virtual Threads and Structured Concurrency

### 15.1 What changed (Java 21)

Platform threads map 1:1 to OS threads: ~1MB stack, expensive to create, thousands is a lot. Virtual threads are scheduled by the JVM onto a small pool of carrier threads. **Millions are feasible.** When a virtual thread blocks on I/O, the JVM unmounts its continuation from the carrier and the carrier picks up other work.

```java
Thread.startVirtualThread(() -> handle(request));

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (Request r : requests) executor.submit(() -> handle(r));
}   // close() waits for all tasks
```

The strategic point: **the thread-per-request model becomes viable again.** The reason we all moved to reactive stacks — thread scarcity — largely goes away. You write straight-line blocking code, and it scales like async code, with stack traces and debuggers that actually work.

### 15.2 The rules that change

**1. Do not pool virtual threads.** They are cheap and disposable. `newVirtualThreadPerTaskExecutor()` is not a pool — it's a task-per-thread factory. Pooling them reintroduces the scarcity you just eliminated.

**2. `synchronized` used to pin the carrier.** Through Java 21–23, a virtual thread blocking inside a `synchronized` block pinned its carrier thread, and enough pinning deadlocks the scheduler. The remedy was `ReentrantLock` on any path that blocks. JDK 24+ (JEP 491) removes this limitation. **Check your runtime version before relying on either behaviour** — this is the single most version-sensitive fact in modern Java concurrency.

**3. `ThreadLocal` still works but is a footgun at scale.** A million virtual threads × a heavy thread-local context = a memory problem. Use `ScopedValue`.

**4. They do not make CPU-bound work faster.** The carrier pool is still `cores`-sized. Virtual threads solve *blocking*, not *computation*.

**5. Native frames still block.** JNI calls and a few legacy APIs pin the carrier.

### 15.3 Structured concurrency

Concurrent subtasks get a lifetime bound to a lexical scope — no leaked threads, unified cancellation, proper error propagation.

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<User>  user  = scope.fork(() -> userService.find(id));
    Subtask<Score> score = scope.fork(() -> scoreService.fetch(id));

    scope.join();               // wait for all
    scope.throwIfFailed();      // propagate the first failure

    return new Dashboard(user.get(), score.get());
}   // any still-running subtask is cancelled on scope exit
```

If `userService` fails, `scoreService` is cancelled immediately instead of running to completion pointlessly. `ShutdownOnSuccess` gives you the opposite: first result wins, cancel the rest — the hedged-request pattern in six lines.

*(API preview status varies by JDK — verify against your target version.)*

### 15.4 Enabling in Spring Boot

```properties
spring.threads.virtual.enabled=true
```

This switches Tomcat's request handling and `@Async` to virtual threads. Before you flip it: audit for `synchronized` around blocking calls (if on JDK 21–23), confirm your DB connection pool is now the real bottleneck (it will be — HikariCP still has N connections, and unlimited virtual threads just means a longer connection queue), and re-tune any rate limiting that implicitly relied on thread-pool size as a concurrency cap.

---
---

# Part V — In Production

## 16. Liveness Failures

Safety failures mean *something bad happened*. Liveness failures mean *nothing good will ever happen*.

### 16.1 Deadlock

Four conditions must all hold (Coffman):
1. **Mutual exclusion** — a resource is held exclusively.
2. **Hold and wait** — a thread holds one resource while requesting another.
3. **No preemption** — resources can't be forcibly taken.
4. **Circular wait** — a cycle in the wait-for graph.

Break any one and deadlock is impossible.

**Lock ordering — the standard fix.** Break condition 4 by imposing a global order on lock acquisition.

```java
// BROKEN: transfer(A,B) and transfer(B,A) concurrently => deadlock
void transfer(Account from, Account to, long amt) {
    synchronized (from) { synchronized (to) { ... } }
}

// FIXED: always lock in a consistent global order
void transfer(Account from, Account to, long amt) {
    Account first  = from.id() < to.id() ? from : to;
    Account second = from.id() < to.id() ? to   : from;
    synchronized (first) {
        synchronized (second) {
            from.debit(amt); to.credit(amt);
        }
    }
}
```

If there's no natural ordering key, use `System.identityHashCode` with a tie-breaker lock for the (rare) collision case.

**`tryLock` with timeout — breaks hold-and-wait.** Acquire both or release both and retry with backoff. Costs you a retry loop; buys you a system that recovers.

**The subtle one: open vs alien calls.** Calling an unknown method while holding a lock is how deadlocks appear in code that looks fine.

```java
synchronized (this) {
    for (Listener l : listeners) l.onEvent(e);   // alien code, lock held
}
```

You have no idea what locks `onEvent` takes. Copy the collection, release the lock, then call out:

```java
List<Listener> snapshot;
synchronized (this) { snapshot = List.copyOf(listeners); }
for (Listener l : snapshot) l.onEvent(e);        // open call
```

(Or use `CopyOnWriteArrayList` and skip the copy.)

**Resource deadlocks** don't involve locks at all: two thread pools that submit work to each other and wait, or a task in a pool that blocks on a `Future` produced by the same single-threaded pool. Sizing a pool at 1 and having tasks depend on tasks is a guaranteed hang.

**Detection:** `jstack <pid>` prints `Found one Java-level deadlock:` with both stacks. `ThreadMXBean.findDeadlockedThreads()` lets you detect it programmatically and alert.

### 16.2 Livelock

Threads are running but making no progress — each responding to the other's action. Classic in retry loops where all clients retry after the same fixed delay, collide again, and repeat. **Fix: randomized exponential backoff with jitter.**

```java
long delay = Math.min(baseMs * (1L << attempt), maxMs);
long jittered = ThreadLocalRandom.current().nextLong(delay / 2, delay);
Thread.sleep(jittered);
```

### 16.3 Starvation

A thread perpetually loses the race for a resource. Causes: unfair locks under sustained contention, thread priorities (unreliable and OS-dependent — don't use them), or a small pool where long tasks crowd out short ones. Fixes: fair locks, separate pools by task class, or a priority queue with aging.

---

## 17. Performance and Contention

### 17.1 Amdahl's law

```
Speedup ≤ 1 / (S + (1 − S)/N)
```

With 5% serial work, the ceiling is 20× no matter how many cores. Every lock, every `synchronized` block, every shared counter contributes to S. **Reducing serialization beats adding threads.**

### 17.2 The costs

- **Context switch:** ~1–10μs of direct cost, plus cache pollution that often costs more.
- **Uncontended lock:** tens of nanoseconds. Effectively free.
- **Contended lock:** the thread parks and unparks — microseconds, plus scheduler involvement.
- **Cache line transfer** between cores: ~100ns.

Consequence: **uncontended synchronization is not the problem.** Don't remove `synchronized` for performance without a profiler telling you it's contended. The JIT elides locks it can prove are thread-confined (lock elision) and merges adjacent blocks (lock coarsening).

### 17.3 Reducing contention

1. **Reduce lock duration.** Move I/O, logging, and pure computation out of the critical section.
2. **Reduce lock frequency.** Batch updates.
3. **Lock splitting.** One lock per independent invariant instead of one lock for the whole object.
4. **Lock striping.** N locks over a partitioned data structure — what `ConcurrentHashMap` does.
5. **Eliminate the shared state.** Per-thread accumulators merged at the end (`LongAdder`'s strategy, and the whole idea behind stream reduction).

### 17.4 False sharing

Two independent variables on the same 64-byte cache line. Thread A writes one, invalidating the line for thread B — who never touched A's variable. Pure overhead, invisible in code review.

```java
// Java 8+: the JVM adds padding
@jdk.internal.vm.annotation.Contended
static class Cell { volatile long value; }
```

(`@Contended` requires `-XX:-RestrictContended` outside the JDK. `LongAdder`'s cells use exactly this.) Diagnose with `perf c2c` on Linux, not by guessing.

---

## 18. Testing and Debugging

Concurrency bugs are probabilistic. A test that passes 1,000 times has proved very little.

### 18.1 What actually works

**Stress + assertion of invariants.** Run many threads, then check that global invariants hold:

```java
@Test
void concurrentIncrementsAreNotLost() throws Exception {
    Counter counter = new Counter();
    int threads = 32, perThread = 10_000;
    var pool = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var done  = new CountDownLatch(threads);

    for (int i = 0; i < threads; i++) {
        pool.submit(() -> {
            start.await();                      // maximize collision window
            for (int j = 0; j < perThread; j++) counter.increment();
            done.countDown();
            return null;
        });
    }
    start.countDown();
    assertTrue(done.await(30, TimeUnit.SECONDS));
    assertEquals(threads * perThread, counter.get());   // the invariant
    pool.shutdownNow();
}
```

The `CountDownLatch` start gate matters: without it, threads start staggered and rarely collide.

**jcstress** — the JDK's harness for memory-model tests. It generates the interleavings and records which outcomes occurred. If you're writing lock-free code, this is the only serious option.

**Deterministic tests where possible.** Inject a `Clock`, inject the `Executor`. A test that passes a same-thread executor is deterministic and tests your *logic*; a separate stress test covers the *concurrency*. Don't try to do both in one test.

**Never use `Thread.sleep` to synchronize a test.** It's a flaky test generator. Use latches, `Awaitility`, or a controllable executor.

### 18.2 Reading a thread dump

```bash
jstack <pid> > dump.txt
jcmd <pid> Thread.print
kill -3 <pid>                    # dump to stdout
```

Take **three dumps, 5–10 seconds apart.** One dump is a photo; three is a movie. Threads stuck in the same frame across all three are the problem.

What to look for:

| Pattern | Meaning |
|---------|---------|
| Many `BLOCKED` on the same `<0x...>` monitor | Lock contention; find the one thread holding it |
| Many `WAITING` on a pool's `SynchronousQueue` | Idle pool — fine |
| Many `WAITING` on `HikariPool.getConnection` | Connection pool exhausted; threads > connections |
| `RUNNABLE` ending in `socketRead0` | Blocked on network I/O, not CPU |
| `Found one Java-level deadlock` | Deadlock, with both cycles printed |
| Thread count growing between dumps | Thread leak — a pool created per request |

### 18.3 The rest of the toolkit

- **`jcmd <pid> Thread.print -l`** — includes ownable synchronizer (`ReentrantLock`) info that plain `jstack` omits.
- **Async-profiler in lock mode** (`-e lock`) — a flame graph of *contention*, which tells you where to actually optimize.
- **JFR** — `jdk.JavaMonitorEnter` and `jdk.ThreadPark` events, low enough overhead for production.
- **`-XX:+PrintCompilation`, `-Xint`** — a bug that vanishes under `-Xint` is a JIT-reordering/visibility bug, i.e. a missing `volatile`.
- **`ThreadMXBean`** — expose deadlock detection and thread counts as health-check metrics.

---

## 19. Concurrency in a Spring Boot Service

Everything above, applied to the environment most of this actually runs in.

### 19.1 Beans are singletons and shared

Every request thread hits the same `@Service` instance. Therefore:

```java
@Service
public class PaymentService {
    private final PaymentRepository repo;      // fine: stateless, thread-safe
    private int requestCount;                  // BUG: mutable shared state
    private SimpleDateFormat fmt;              // BUG: not thread-safe

    // fine: everything else is a local variable, confined to the calling thread
}
```

Keep beans stateless. If you need per-request state, use method locals or a request-scoped bean. If you need shared counters, use `LongAdder` or a `MeterRegistry`.

`@Scope("prototype")` on a bean injected into a singleton does **not** give you a new instance per call — it's resolved once at injection. Use `ObjectProvider<T>` or a lookup method.

### 19.2 `@Async`

```java
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Override public Executor getAsyncExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(200);                       // bounded
        ex.setThreadNamePrefix("async-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);   // graceful
        ex.setAwaitTerminationSeconds(30);
        ex.setTaskDecorator(new MdcTaskDecorator());    // propagate logging context
        ex.initialize();
        return ex;
    }
    @Override public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("async failure in {}", method, ex);
    }
}
```

The traps:
- **Self-invocation doesn't work.** `@Async` is proxy-based; calling `this.asyncMethod()` from within the same bean runs synchronously. Same root cause as the `@Transactional` self-invocation trap. Inject the bean into itself or split the class.
- **The default executor is `SimpleAsyncTaskExecutor`** in older setups — it creates a **new thread per call, unbounded.** Always configure your own.
- **`void` async methods swallow exceptions** unless you register the handler above. Return `CompletableFuture<T>` where you care.
- **`@Async` + `@Transactional`** — the transaction does not propagate across the thread boundary. The async method needs its own.

### 19.3 The connection pool is your real concurrency limit

```
tomcat.threads.max = 200
hikari.maximum-pool-size = 10
```

200 request threads competing for 10 connections. 190 threads sit in `HikariPool.getConnection`, requests time out, and CPU looks idle the whole time. The classic misdiagnosis is "we need more threads."

Sane starting point: `pool size ≈ (cores × 2) + effective_spindle_count`, and make sure `connectionTimeout` is shorter than your upstream request timeout so you fail fast rather than piling up.

**A transaction that makes an HTTP call holds a DB connection for the duration of that call.** This one pattern has caused more pool-exhaustion incidents than anything else. Move the external call outside the transaction boundary.

### 19.4 Database-level concurrency

In-JVM locks are worthless when you run three replicas. The database is your shared-state arbiter.

**Optimistic locking** — default choice, no blocking:

```java
@Entity
public class Wallet {
    @Id private String id;
    @Version private long version;      // JPA increments and checks on update
    private BigDecimal balance;
}
// Concurrent update => OptimisticLockException => retry
```

**Pessimistic locking** — when contention is genuinely high and retries would thrash:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select w from Wallet w where w.id = :id")
Optional<Wallet> findByIdForUpdate(@Param("id") String id);   // SELECT ... FOR UPDATE
```

Always set a lock timeout. And lock rows in a consistent order across transactions — row-level deadlocks obey the same rules as §16.1, and PostgreSQL will kill one transaction with a deadlock error.

**Atomic SQL** — often the best answer is no application-level locking at all:

```sql
UPDATE wallet SET balance = balance - :amt
WHERE id = :id AND balance >= :amt;    -- 0 rows updated = insufficient funds
```

One statement, atomic, no read-modify-write, no race. Check the affected row count.

**Idempotency** beats locking for request-level dedup:

```sql
INSERT INTO txn (idempotency_key, ...) VALUES (:key, ...)
ON CONFLICT (idempotency_key) DO NOTHING;
```

A unique constraint is a distributed lock that already works, is durable, and costs nothing extra.

**Note on isolation levels.** `READ COMMITTED` (PostgreSQL's default) does not prevent lost updates across a read-then-write in application code. `REPEATABLE READ` in PostgreSQL will abort the second transaction with a serialization failure — which means your application must be prepared to retry. Isolation level is a concurrency control decision, not a database setting you inherit and forget.

### 19.5 Kafka and consumer concurrency

- **Order is per-partition.** Ordering guarantees only exist within a partition — so the partition key determines what's serialized. Keying by `userId` or `accountId` means all events for one entity are processed in order by one consumer, which removes an entire class of concurrency problem by construction.
- `KafkaListener` `concurrency = N` creates N consumer threads, capped usefully at the partition count.
- **A `KafkaListener` method must be thread-safe** if concurrency > 1 — same rules as any shared bean.
- **Don't fan a single record out to an async pool and then ack.** You've just converted at-least-once into at-most-once and lost your ordering guarantee.

### 19.6 Distributed locks

When you truly need mutual exclusion across JVMs:

```java
RLock lock = redisson.getLock("settlement:" + batchId);
if (lock.tryLock(2, 30, TimeUnit.SECONDS)) {     // wait 2s, lease 30s
    try { runSettlement(batchId); } finally { lock.unlock(); }
}
```

Understand what you're buying. A Redis lock is **not** a correctness guarantee under network partition — the lease can expire while your work is still running, and two nodes will believe they hold it. Redlock's safety is actively disputed. Use distributed locks for *efficiency* (avoid duplicate work), and enforce *correctness* with something that fences: a monotonic fencing token checked at the write, or a DB unique constraint / conditional update.

Always: bounded lease, always release in `finally`, and design the critical section to be idempotent so a double execution is survivable.

---

## 20. The Interview Playbook

### 20.1 How to answer any "make this thread-safe" question

1. **Name the shared mutable state.** Out loud. "The shared state here is the `count` field and the `map`."
2. **Name the invariant.** "The invariant is that `size` always matches the number of entries."
3. **Walk the ladder** (§4). "Can this be immutable? Confined? Can I delegate to a `ConcurrentHashMap`?"
4. **Choose the smallest tool that covers the whole invariant.**
5. **State the tradeoff.** "This serializes all writers; if write throughput is the constraint, I'd stripe by key."
6. **Mention the failure mode you're avoiding.** Lost update, stale read, deadlock — by name.

Skipping straight to "add `synchronized`" is the answer that doesn't distinguish you from anyone else.

### 20.2 Problems worth implementing by hand

| Problem | The concept it tests |
|---------|---------------------|
| Bounded blocking queue | `wait`/`notify`, two `Condition`s, why `while` not `if` |
| Thread-safe LRU cache | Lock choice, `LinkedHashMap` vs `ConcurrentHashMap` + list, striping |
| Rate limiter (token bucket, sliding window) | Atomics vs locks, clock handling, distributed variant |
| Print in order / odd-even / `FooBar` | Condition signalling, avoiding busy-wait |
| Dining philosophers | Lock ordering, `tryLock`, resource hierarchy |
| Thread-safe singleton | DCL, `volatile`, holder idiom, enum |
| Task scheduler with delays | `DelayQueue`, `ScheduledExecutorService` internals |
| Read-write lock from scratch | AQS mental model, writer starvation |
| Connection pool | `Semaphore`, borrow/return, leak handling, validation |
| Producer-consumer pipeline | Backpressure, poison pills, graceful shutdown |
| `H2O` / barrier problems | `CyclicBarrier`, `Semaphore` combinations |

### 20.3 High-frequency questions and the crisp answer

**"`volatile` vs `synchronized`?"** — `volatile` gives visibility + ordering for a single variable, no atomicity, no blocking. `synchronized` gives mutual exclusion + visibility + ordering over a region. Use `volatile` for flags and safe publication; `synchronized` when an invariant spans operations.

**"Why is `String` immutable?"** — Safe sharing across threads without synchronization, caching of the hash code, security (a path or connection string can't be mutated after validation), and interning.

**"`ConcurrentHashMap` vs `Hashtable`?"** — `Hashtable` locks the whole map on every operation. CHM locks per bin, reads are lock-free, and it offers atomic compound operations. CHM's iterators are weakly consistent; `Hashtable`'s are fail-fast.

**"Can you make a `HashMap` thread-safe with `synchronized`?"** — You can make access safe, but you get one global lock and no atomic compound operations. Also worth naming: a `HashMap` mutated concurrently without synchronization could historically produce an infinite loop during resize in Java 7. Use `ConcurrentHashMap`.

**"What does `Thread.sleep` do to locks?"** — Nothing. It holds every lock it has. `Object.wait()` releases the monitor it was called on (and only that one).

**"Difference between `submit` and `execute`?"** — `submit` returns a `Future` and captures exceptions into it (silently, if you never call `get`); `execute` returns void and routes exceptions to the `UncaughtExceptionHandler`.

**"Is `i++` atomic on a `volatile long`?"** — No. `volatile` makes 64-bit reads/writes atomic (which they aren't guaranteed to be otherwise for `long`/`double`), but read-modify-write is still three steps.

**"How do you stop a thread?"** — You ask it to stop: set a `volatile` flag or interrupt it, and have the thread check. `Thread.stop()` is deprecated because it throws asynchronously and leaves invariants broken.

**"How would you design a thread pool?"** — Bounded work queue, core/max threads, a worker loop that takes from the queue, keep-alive timeout for surplus threads, a rejection policy, and a two-phase shutdown. Then discuss sizing and the tradeoff between queue depth and thread count.

---
---

# Appendix A: Decision Tables

### Choosing a synchronization mechanism

```
Is the state shared across threads?
├── No  → nothing to do (thread confinement)
└── Yes → Is it mutated after publication?
    ├── No  → make it immutable / final; safe publication is enough
    └── Yes → Does the invariant span more than one variable?
        ├── No  → Is it a read-modify-write?
        │   ├── No  → volatile
        │   └── Yes → Is it a pure counter with heavy writes?
        │       ├── Yes → LongAdder
        │       └── No  → AtomicX
        └── Yes → Is there a concurrent collection that models it?
            ├── Yes → use it (CHM, BlockingQueue, ...)
            └── No  → Do you need timeout / interrupt / multiple conditions?
                ├── No  → synchronized on a private lock
                └── Yes → ReentrantLock (+ Condition)
```

### Choosing an execution mechanism

| Workload | Mechanism |
|----------|-----------|
| CPU-bound, N ≈ cores | Fixed pool sized `cores` |
| CPU-bound, divide-and-conquer over memory | `ForkJoinPool` / `RecursiveTask` |
| I/O-bound, moderate count, JDK ≤ 17 | Explicit `ThreadPoolExecutor`, sized by wait/compute ratio |
| I/O-bound, high count, JDK 21+ | Virtual threads, one per task |
| Async composition of several calls | `CompletableFuture` with a supplied executor |
| Async composition, JDK 21+, want cancellation | `StructuredTaskScope` |
| Periodic / delayed | `ScheduledThreadPoolExecutor` (wrap the body in try/catch) |
| Event-driven, ordered per key | Kafka partitioned by key — no in-process concurrency at all |

---

# Appendix B: Bug Gallery

| # | Bug | Symptom | Fix |
|---|-----|---------|-----|
| 1 | `count++` on a shared field | Counts drift low under load | `AtomicInteger` / `LongAdder` |
| 2 | Non-volatile stop flag | Loop never exits | `volatile` |
| 3 | DCL without `volatile` | Rare `NullPointerException` on a field of a non-null object | `volatile`, or holder idiom |
| 4 | `if` instead of `while` around `wait()` | Rare corruption after a spurious wakeup | `while` |
| 5 | Swallowed `InterruptedException` | Shutdown hangs; pod hits grace-period timeout | Restore the flag or rethrow |
| 6 | Unbounded queue in a pool | `OutOfMemoryError` hours into a traffic spike | Bounded queue + rejection policy |
| 7 | `submit()` with no `get()` | Task fails silently; no logs | `UncaughtExceptionHandler`, or handle the `Future` |
| 8 | Periodic task throws | Job stops running, no error | try/catch inside the task body |
| 9 | `ThreadLocal` not removed | Slow heap growth, classloader leak | `remove()` in `finally` |
| 10 | Lock held across an HTTP call | p99 latency cliff, threads all `BLOCKED` | Shrink the critical section |
| 11 | Inconsistent lock order | Deadlock under concurrent transfers | Global lock ordering |
| 12 | Alien call under lock | Deadlock in listener/callback code | Open call — copy, release, then call |
| 13 | Side effect inside `compute`/`updateAndGet` | Duplicate writes under contention | Keep the function pure |
| 14 | `SimpleDateFormat` in a static field | Garbage dates, occasional exceptions | `DateTimeFormatter` (immutable) |
| 15 | Blocking I/O in a parallel stream | Unrelated parts of the app stall | Dedicated pool, or don't |
| 16 | `@Async` self-invocation | Runs synchronously; nobody notices for months | Split the bean or self-inject |
| 17 | Threads ≫ DB connections | Timeouts with idle CPU | Size the pool to the real bottleneck |
| 18 | `this` escapes the constructor | Another thread sees partially built object | Static factory |
| 19 | Missing `unlock()` on an exception path | Permanent deadlock | `try { } finally { unlock(); }` |
| 20 | Fixed retry delay across clients | Livelock / thundering herd | Exponential backoff with jitter |

---

# Appendix C: Cheat Sheet

**Happens-before edges:** program order · unlock→lock · volatile write→read · `start()` · `join()` · `interrupt()` · transitivity.

**Safe publication:** static initializer · `volatile`/`AtomicReference` · `final` field · guarded by a lock.

**`volatile` is enough only if:** writes don't depend on the current value, no invariant with other fields, no lock needed for other reasons.

**The blocking-queue method grid:** `put`/`take` block · `offer`/`poll` return a sentinel · `add`/`remove` throw · timed `offer`/`poll` wait.

**Pool task arrival:** core → queue → max → reject. *Unbounded queue ⟹ max is never reached.*

**Always in a `finally`:** `unlock()` · `release()` · `countDown()` · `remove()` on a `ThreadLocal` · `MDC.clear()`.

**Always bounded:** work queues · thread pools · retry counts · lock lease times · timeouts on every `await`/`get`/`tryLock`.

**Never:** hold a lock across I/O · call alien code under a lock · use `Thread.stop`/`suspend` · swallow `InterruptedException` · assume `notify()` is safe with heterogeneous waiters · use `Thread.sleep` to synchronize a test · pool virtual threads · trust `size()` on a concurrent collection.

**Debug in this order:** three thread dumps 5s apart → find `BLOCKED` clusters and the monitor owner → check pool queue depths and connection-pool waits → async-profiler in lock mode → JFR in production.

**The one-line summary:** *Don't share mutable state. If you must share, make it immutable. If it must be mutable, confine it. If it must be shared and mutable, put every operation on the invariant behind exactly one lock — and hold that lock for as short a time as you possibly can.*

---

*End of book.*
