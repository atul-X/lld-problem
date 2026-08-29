# Java Concurrency — Coding Problem List
### Locks, threads, and multithreading. Every item is something you write code for.

**How to use this:** don't read solutions. Write each one in a scratch project with a `main()` that spawns real threads, run it 10,000 times in a loop, and assert an invariant at the end. A concurrency solution that "looks right" and one that *is* right are indistinguishable until you stress it.

Companion reference: the Java Concurrency & Thread Safety book from the previous chat — Ch. 9 (Condition), Ch. 10 (synchronizers), Ch. 11 (concurrent collections) cover most of the primitives below.

---

## Tier 1 — LeetCode Concurrency (all 9 problems)

This is the complete set — nine problems. Small, but they are the highest signal-per-minute concurrency practice available, and interviewers pull from them directly.

Problem set: https://leetcode.com/problemset/concurrency/

| # | Problem | Link | What it forces you to learn |
|---|---------|------|------------------------------|
| 1114 | Print in Order | https://leetcode.com/problems/print-in-order/ | The simplest possible ordering constraint. Solve it **four ways**: `CountDownLatch`, `Semaphore`, `synchronized` + `wait/notifyAll`, `ReentrantLock` + `Condition`. This single problem is your primitive tour. |
| 1115 | Print FooBar Alternately | https://leetcode.com/problems/print-foobar-alternately/ | Strict alternation between two threads. Two `Semaphore`s (one starting at 1, one at 0) is the clean answer. Teaches you the two-semaphore handoff. |
| 1116 | Print Zero Even Odd | https://leetcode.com/problems/print-zero-even-odd/ | Three threads, one interleaving pattern (`0102030405`). Extension of 1115 to a cycle. Teaches you to model state as "whose turn is it" rather than as ad-hoc flags. |
| 1117 | Building H2O | https://leetcode.com/problems/building-h2o/ | Barrier semantics: two H and one O must group before any proceed. `Semaphore` + `CyclicBarrier(3)` is the elegant answer. Teaches grouping/rendezvous. |
| 1188 | Design Bounded Blocking Queue *(premium)* | https://leetcode.com/problems/design-bounded-blocking-queue/ | **The single most important one.** Producer-consumer with capacity. Do it with `ReentrantLock` + two `Condition`s. If you can write this from memory you can answer half of all concurrency interviews. |
| 1195 | Fizz Buzz Multithreaded | https://leetcode.com/problems/fizz-buzz-multithreaded/ | Four threads, conditional turn-taking. Teaches you why a single `Condition` + `signalAll` + `while` re-check beats four hand-rolled flags. |
| 1226 | The Dining Philosophers | https://leetcode.com/problems/the-dining-philosophers/ | **Deadlock, by name.** Solve it three ways: global lock ordering, `tryLock` with release-and-retry, and a `Semaphore(n-1)` limiting concurrent diners. The canonical deadlock-prevention question. |
| 1242 | Web Crawler Multithreaded *(premium)* | https://leetcode.com/problems/web-crawler-multithreaded/ | Shared visited-set + work queue + termination detection. Teaches `ConcurrentHashMap.newKeySet()`, and the genuinely hard part: **knowing when everyone is done.** |
| 1279 | Traffic Light Controlled Intersection *(premium)* | https://leetcode.com/problems/traffic-light-controlled-intersection/ | Mutual exclusion on a shared resource with state. Easy once you see it's a mutex; the point is recognizing it. |

**If you do only three:** 1188 (bounded queue), 1226 (dining philosophers), 1114 (four ways).

Reference solutions if you get stuck (look *after* attempting):
- https://walkccc.me/LeetCode/topics/system-design/concurrency/
- https://github.com/varunu28/LeetCode-Java-Solutions/blob/master/Concurrency/README.md

---

## Tier 2 — Classic synchronization problems (implement in Java)

No LeetCode links; these are OS-textbook problems that show up verbatim in interviews at Rubrik, Arcesium, Oracle, Uber, and most trading firms. Source for statements, hints, and solutions: **The Little Book of Semaphores** (free) — https://greenteapress.com/wp/semaphores/ · direct PDF: https://greenteapress.com/semaphores/LittleBookOfSemaphores.pdf

| Problem | Statement in one line | Primitive it teaches |
|---------|----------------------|----------------------|
| **Producer–Consumer (bounded buffer)** | N producers, M consumers, fixed-size buffer. | `wait`/`notifyAll`, then redo with two `Condition`s. The foundation problem. |
| **Readers–Writers (three variants)** | Many readers OR one writer. Do it (a) reader-preferring, (b) writer-preferring, (c) fair/no-starvation. | `ReentrantReadWriteLock` — then build it yourself from `Semaphore` + counter. Variant (c) is the real interview question. |
| **Dining Philosophers** | See LC 1226. Also do the **Chandy/Misra** solution. | Deadlock prevention vs avoidance. |
| **Sleeping Barber** | One barber, N waiting chairs, customers leave if full. | `Semaphore` triple + bounded waiting room. Models a connection pool exactly. |
| **Cigarette Smokers** | Agent puts out 2 of 3 ingredients; the smoker with the third proceeds. | Why you cannot solve some problems without a "pusher" thread. Genuinely tricky. |
| **Barbershop / Unisex Bathroom** | Categorical exclusion — men OR women inside, not both. | Turnstile pattern, starvation avoidance. |
| **Searcher–Inserter–Deleter** | Searchers concurrent, inserters pairwise-exclusive, deleters fully exclusive. | Three-tier lock hierarchy. Frequently asked at Rubrik. |
| **Water/Hydrogen–Oxygen** | See LC 1117. | Barrier. |
| **Santa Claus problem** | Santa wakes for 9 reindeer or 3 elves; reindeer take priority. | Priority between two waiting groups without starvation. |
| **Building a Semaphore from a Mutex + Condition** | Implement `acquire`/`release` yourself. | Proves you actually understand what a semaphore *is*. |
| **The Barrier problem (reusable)** | N threads rendezvous repeatedly. | Why a naive barrier breaks on the second round — the two-phase turnstile. |

Also worth having in the same repo: **The Dining Philosophers with `tryLock` + timeout + metrics**, since that's the production-shaped version of the answer.

---

## Tier 3 — Build the primitive from scratch

Interviewers love these because there's nowhere to hide. Rule for all of them: **you may only use `synchronized` / `wait` / `notify` / `ReentrantLock` / `Condition`. No `java.util.concurrent` collections, no `Atomic*`.**

| # | Build this | The trap it's testing |
|---|-----------|----------------------|
| 1 | `BoundedBlockingQueue<T>` with `put`/`take`/timed `offer`/timed `poll` | `while` not `if`; two conditions; correct timeout arithmetic with `awaitNanos` returning remaining time |
| 2 | A **thread pool** — `submit(Runnable)`, N workers, bounded queue, `shutdown()` and `shutdownNow()` | Worker loop, poison pills vs interruption, draining the queue, `awaitTermination` |
| 3 | A **read-write lock** | Writer starvation. Also: can you implement downgrade? Why is upgrade impossible? |
| 4 | A **reentrant lock** | Owner tracking + hold count. Then add `tryLock(timeout)`. |
| 5 | A **counting semaphore** with fairness | FIFO wait queue, barging |
| 6 | A **`CountDownLatch`** and then a **reusable `CyclicBarrier`** | Why the reusable one needs generation tracking |
| 7 | A **`DelayQueue`** / delayed task scheduler | Priority queue + `awaitNanos` on the head's delay; correctly waking when a *sooner* task is added |
| 8 | A **`Future` / `Promise`** — `complete(v)`, blocking `get()`, `get(timeout)`, callbacks | One-shot state machine, callback-on-already-completed |
| 9 | A **lock-free stack** using `AtomicReference` | ABA — then fix it with `AtomicStampedReference` |
| 10 | A **thread-safe counter, 4 ways**, then benchmark them | `synchronized` vs `AtomicLong` vs `LongAdder` vs striped. Actually measure with JMH. |
| 11 | An **`Exchanger`** (two-party swap) | Rendezvous with paired state |
| 12 | A **rate-limited `Semaphore`** that refills over time | Combining a scheduler with a permit pool |

---

## Tier 4 — LLD / machine-coding rounds where concurrency *is* the question

This is the tier that matters most for SDE-3. These are 45–90 minute design-and-code problems where thread safety is the grading criterion, not a footnote.

| # | Problem | Concurrency decision you must defend |
|---|---------|--------------------------------------|
| 1 | **Thread-safe LRU cache** with capacity eviction | `ConcurrentHashMap` + doubly-linked list is *not* trivially safe — the list needs its own lock. Discuss: single lock vs striping vs `ConcurrentLinkedHashMap`/Caffeine's approach. Add TTL for the follow-up. |
| 2 | **Rate limiter** — token bucket, leaky bucket, fixed window, sliding window log, sliding window counter | Per-key locking vs `AtomicLong` CAS loop vs `computeIfAbsent`. Then: how does it work across 3 replicas? (Redis Lua, atomic INCR+EXPIRE.) |
| 3 | **Connection pool** | `Semaphore` for capacity + `BlockingQueue` for idle connections; leak detection; validation on borrow; fair vs unfair. Compare against HikariCP's `ConcurrentBag`. |
| 4 | **Task scheduler** (`schedule`, `scheduleAtFixedRate`, `cancel`) | `DelayQueue` or `PriorityBlockingQueue` + a dispatcher thread. Handling a task that overruns its period. Why a throwing periodic task must not kill the schedule. |
| 5 | **In-memory key-value store with TTL** | Lazy vs active expiry; a background sweeper thread; `ConcurrentHashMap.compute` for atomic get-and-refresh. |
| 6 | **Transactional / versioned KV store** | MVCC in miniature — `AtomicReference` to an immutable snapshot, optimistic retry on conflict. |
| 7 | **Producer–consumer log processing pipeline** (multi-stage) | Bounded queues between stages, backpressure, graceful shutdown with poison pills, ordering guarantees. |
| 8 | **Notification/event dispatcher with retry + DLQ** | Retry with jitter, at-least-once vs exactly-once, idempotency keys, per-key ordering. |
| 9 | **Elevator system** | Multiple elevator threads + a shared request queue + a scheduling policy. Classic Uber/Amazon LLD. |
| 10 | **Parking lot with concurrent entry/exit** | Where exactly is the race? (Slot allocation.) Optimistic vs pessimistic, and what happens at the gate under 100 rps. |
| 11 | **Order matching engine** | Per-symbol single-threaded processing (the trick), price-level priority queues, sequence numbers. |
| 12 | **Distributed ID generator (Snowflake)** | Clock going backwards, sequence overflow within a millisecond, `synchronized` vs CAS on the counter. |
| 13 | **Bank account / wallet transfer** | Deadlock via lock ordering; then the real answer — atomic SQL `UPDATE ... WHERE balance >= amt`, and idempotency keys. |
| 14 | **Concurrent file downloader** with byte-range chunks | Fan-out, partial failure and retry per chunk, ordered reassembly, progress reporting. |
| 15 | **`H2O`-style resource pool with priority** | Starvation of the low-priority class; aging. |

For #1–#4, aim to write them cold in under 40 minutes each. Those four cover the majority of what gets asked.

---

## Tier 5 — Build-it projects (weekend-sized, best interview stories)

| Project | The concurrency lesson |
|---------|------------------------|
| **Multithreaded web crawler** with a politeness delay per domain | Termination detection (when is the frontier truly empty?), per-domain rate limiting, dedup at scale |
| **Parallel merge sort / word count with Fork/Join** | Threshold tuning, `RecursiveTask`, work stealing — then benchmark against sequential and be honest about when it loses |
| **Mini MapReduce** over local files | Partitioning, shuffle, combining, `CompletableFuture.allOf` |
| **Parallel `grep` across a directory tree** | I/O-bound sizing, virtual threads vs a fixed pool — measure both |
| **A tiny HTTP server** (thread-per-request, then pooled, then virtual threads) | The whole arc of Ch. 12→15 in one codebase |
| **Reproduce the classic bugs deliberately** | Write a lost-update, a stale-flag infinite loop, and a broken double-checked-lock. Then prove the fixes with **jcstress**: https://openjdk.org/projects/code-tools/jcstress/ |

---

## Suggested order (about 4 weeks, ~1 hour/day)

**Week 1 — primitives.** LC 1114 (all four ways) → 1115 → 1116 → 1195. Then Tier 3 #10 (counter, 4 ways, benchmarked).

**Week 2 — the core patterns.** LC 1188 → Tier 3 #1 (bounded queue from scratch) → Producer-Consumer → Readers-Writers (all three variants) → LC 1117.

**Week 3 — deadlock and pools.** LC 1226 (three solutions) → Sleeping Barber → Tier 3 #2 (thread pool) and #7 (delay queue) → Tier 4 #3 (connection pool) and #4 (scheduler).

**Week 4 — LLD under time pressure.** Tier 4 #1 (LRU), #2 (rate limiter), #13 (wallet transfer), #7 (pipeline). 45-minute timer, no reference material, write a stress test for each.

---

## Reference links

| Resource | Link |
|----------|------|
| LeetCode concurrency problem set | https://leetcode.com/problemset/concurrency/ |
| The Little Book of Semaphores (free PDF) | https://greenteapress.com/wp/semaphores/ |
| Jenkov's Java Concurrency tutorial | https://jenkov.com/tutorials/java-concurrency/index.html |
| jcstress — the JDK's memory-model test harness | https://openjdk.org/projects/code-tools/jcstress/ |
| LeetCode community concurrency prep guide | https://leetcode.com/discuss/post/7605788/surgical-strike-on-concurrency-and-multi-nghz/ |
| Java concurrency solutions (Java) | https://github.com/varunu28/LeetCode-Java-Solutions/blob/master/Concurrency/README.md |
| Solutions with explanations | https://walkccc.me/LeetCode/topics/system-design/concurrency/ |

Books worth owning: *Java Concurrency in Practice* (Goetz) — still the reference despite its age; *The Art of Multiprocessor Programming* (Herlihy & Shavit) for the lock-free chapters.

---

## The one habit that separates a good answer from a great one

For every problem above, after you get it working, ask yourself the three questions an SDE-3 interviewer is waiting for:

1. **What breaks if I swap `notifyAll()` for `notify()`?**
2. **What happens under 10,000 threads instead of 3?**
3. **How does this change when it runs on three replicas instead of one JVM?**

Most candidates get the code right. Very few volunteer the answers to these.
