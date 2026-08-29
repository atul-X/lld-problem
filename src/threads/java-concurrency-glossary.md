# Java Concurrency — Terminology Glossary

*Precise definitions, grouped so that the terms people confuse sit next to each other.*

---

## 1. The Core Five

**Critical section**
A region of code that touches shared mutable state and must not be executed by two threads at once. Not a keyword — it's a property of the code. Your job is to make it as short as possible.

**Mutual exclusion** *(the property)*
The guarantee that at most one thread is inside the critical section at a time. This is the **goal**.

**Mutex** *(the mechanism)*
The thing that provides mutual exclusion. A binary lock: one holder, everyone else waits. In Java: `synchronized`, `ReentrantLock`.

**Lock**
The general term. Acquire before the critical section, release after. A lock has an **owner** — the thread that acquired it is the thread that must release it.

**Semaphore**
A counter of N permits. Allows *up to N* threads through, not just one. Has **no owner** — thread A can acquire and thread B can release.

> ### Mutex vs binary semaphore — the classic trap
> Both allow exactly one thread through, so people call them the same thing. They are not.
>
> | | Mutex | `Semaphore(1)` |
> |---|---|---|
> | Ownership | Yes — only the holder may release | No — any thread may release |
> | Reentrant | Yes (`synchronized`, `ReentrantLock`) | No — re-acquiring self-deadlocks |
> | Can over-release | No | Yes — permits can exceed the initial count |
> | Use it for | Protecting shared state | **Handoff** between two different threads |
>
> The difference is **ownership**, not the count.

---

## 2. How You Wait

**Blocking**
The thread is parked by the OS/JVM and consumes no CPU while waiting. `synchronized` entry, `lock()`, `queue.take()`.

**Busy-waiting / spinning**
The thread loops checking a condition, burning CPU. `while (!flag) { }`. Fast when the wait is nanoseconds; disastrous otherwise. Almost always a bug in application code.

**Pessimistic**
Assume conflict will happen; prevent it with a lock. Nobody else can interfere.
→ `synchronized`, `SELECT ... FOR UPDATE`

**Optimistic**
Assume conflict is rare; let it happen, detect it, retry.
→ CAS, `@Version` in JPA, `StampedLock.tryOptimisticRead()`

**CAS (compare-and-swap)**
A single hardware instruction: *"if this location still holds X, write Y; tell me whether it worked."* The building block of every optimistic and lock-free technique.

**Lock-free**
No locks, and *the system as a whole* always makes progress — some thread completes. An individual thread might retry indefinitely. Example: `AtomicInteger.incrementAndGet()`.

**Wait-free**
Stronger: *every* thread completes in a bounded number of steps. Rare in practice.

---

## 3. What Goes Wrong

**Race condition**
The correctness of the result depends on the timing of threads. **Broader** than a data race — you can have a race condition in perfectly synchronized code (e.g. check-then-act across two individually-atomic calls).

**Data race**
A specific, formal thing: two threads access the same memory location, at least one writes, and there is **no happens-before edge** between them. Undefined behaviour under the Java Memory Model.

> Every data race is a bug. Not every race condition is a data race. `volatile` eliminates data races; it does **not** eliminate race conditions.

**Lost update**
The concrete symptom: two threads read `7`, both write `8`, one increment vanishes.

**Check-then-act**
The shape of most race conditions.
```java
if (!map.containsKey(k)) map.put(k, v);   // the gap is the bug
```

**Read-modify-write**
The other shape. `count++` — three operations pretending to be one.

**Deadlock**
Threads waiting on each other in a cycle. Nobody moves. Requires all four Coffman conditions (mutual exclusion, hold-and-wait, no preemption, circular wait); break the **circular wait** with a global lock ordering.

**Livelock**
Threads *are* running and changing state, but making no progress — each reacting to the other. Two people stepping aside in a corridor. Fix: randomized exponential backoff.

**Starvation**
A thread can run but perpetually loses the race for a resource. Others are making progress; this one isn't. Fix: fairness, or separate pools by task class.

> ### The three liveness failures, distinguished
> - **Deadlock** — everyone is *blocked*.
> - **Livelock** — everyone is *running but futile*.
> - **Starvation** — *some* make progress; one doesn't.

---

## 4. Correctness Properties

**Safety**
*"Nothing bad ever happens."* Invariants hold. Violated by races and corruption.

**Liveness**
*"Something good eventually happens."* Violated by deadlock, livelock, starvation.

These trade off. The trivially safest program never runs; the trivially most live program has no locks and corrupts everything.

**Atomicity**
The operation is indivisible — nobody observes it half-done.

**Visibility**
A write by thread A becomes observable to thread B. **Separate from atomicity**, and the reason `volatile` exists.

**Ordering**
Whether thread B sees A's operations in the order A wrote them. The compiler and CPU reorder freely; only synchronization constrains it.

**Happens-before**
The formal relation that gives you visibility + ordering. If A happens-before B, A's effects are visible to B and appear to occur first. No edge ⟹ no guarantee whatsoever.

**Memory barrier / fence**
The machine-level instruction that enforces ordering. What `volatile` reads/writes and lock acquire/release compile down to.

---

## 5. Threads and State

**Thread-safe**
The class behaves correctly under any interleaving, **with no extra synchronization required from callers**. That last clause is the part people drop.

**Thread confinement**
The state is only ever touched by one thread, so no synchronization is needed. `ThreadLocal`, method locals, single-writer designs, per-partition Kafka consumers.

**Immutable**
State never changes after construction. Automatically thread-safe, no locking ever, safely shareable.

**Safe publication**
Making an object visible to other threads *along with* its fully-initialized state. Without it, another thread can see a non-null reference to a half-built object.

**Reentrant**
A thread already holding the lock can acquire it again.
- Reentrant: `synchronized`, `ReentrantLock`, `ReentrantReadWriteLock`
- **Not** reentrant: `Semaphore`, `StampedLock`

**Contention**
Multiple threads actually competing for the same lock. An *uncontended* lock costs ~20ns and is a non-issue; **contention** is what costs you. Never remove `synchronized` for performance without a profiler showing contention.

**Fairness**
Waiters are granted the resource in FIFO order. Prevents starvation; costs roughly an order of magnitude in throughput because it forbids barging.

**Barging**
A thread that's already running grabs a just-released lock ahead of threads that have been queued waiting. Unfair, and much faster — which is why it's the default.

---

## 6. Quick Disambiguation Table

| These get confused | The actual difference |
|---|---|
| Mutex vs binary semaphore | Ownership and reentrancy, not the count |
| Race condition vs data race | Race condition is broader; data race is the formal "no happens-before edge" case |
| Atomicity vs visibility | Atomicity = indivisible. Visibility = the write is seen at all. `volatile` gives the second, not the first |
| `BLOCKED` vs `WAITING` | `BLOCKED` = lost a race for a monitor. `WAITING` = voluntarily parked, needs a signal |
| Deadlock vs livelock | Blocked vs running-but-futile |
| Lock-free vs wait-free | System-wide progress vs per-thread bounded progress |
| Optimistic vs pessimistic | Detect-and-retry vs prevent-upfront |
| `notify()` vs `notifyAll()` | One arbitrary waiter vs all. `notify()` is safe **only** if all waiters wait on the same condition |
| Concurrency vs parallelism | Concurrency = dealing with many things at once (structure). Parallelism = doing many things at once (execution) |
| Process vs thread | Processes have separate address spaces; threads share heap and share the bugs |

---

## 7. Self-Test

If you can answer these cold, the vocabulary is solid:

1. What's the difference between a race condition and a data race — and why does `volatile` fix one but not the other?
2. Why is a `Semaphore(1)` not a drop-in replacement for `synchronized`?
3. A thread dump shows 180 threads in `WAITING`. Is that a problem? What if they were `BLOCKED`?
4. Name the four Coffman conditions and which one you actually break in practice.
5. `count++` on a `volatile long` — atomic or not? What *does* `volatile` give you on a `long`?
6. Your class has two `AtomicInteger` fields. Is it thread-safe?
7. Why does fairness cost throughput?
