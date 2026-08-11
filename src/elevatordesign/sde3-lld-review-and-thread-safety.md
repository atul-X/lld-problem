# SDE3 LLD Review — Elevator System

## Verdict: Lean No-Hire (borderline)

Strong system-thinking and self-critique (see `elevator-selection-strategy-suggestion.md`).
Weak on delivering against your own stated non-functional requirements — thread safety
above all — and on encapsulation/correctness discipline expected at SDE3.

## What's working

- `elevator-selection-strategy-suggestion.md` shows real system-thinking: you noticed
  `requestElevator(directions, floor, elevatorId)` doesn't model a rider (who never
  picks a car), and derived `ElevatorSelectionStrategy` / `ElevatorPanel` / `HallPanel`
  from first principles. Most candidates never question the parameter list. Keep
  narrating this kind of critique out loud in interviews, even unfinished.
- `SchedulingStrategy` (per-elevator: which floor next) is correctly kept separate from
  elevator *selection* (which car answers a hall call) — different concerns, correctly split.
- Command (`ElevatorRequest implements Command`) and Strategy (`SchedulingStrategy`) are
  applied for real reasons, not decoration.

## Gaps against your own `problem.md`

| Stated requirement (`problem.md`) | Status |
|---|---|
| Command pattern for stop commands | Done |
| Strategy pattern (FIFO, SCAN) | **Fixed** — `SchedulingScan` added alongside `SchedulingFifo` |
| Observer pattern for displays | **Fixed** — `ElevatorObserver`/`ElevatorDisplay`, wired via `Elevator.addObserver` |
| Emergency mode | **Fixed** — `ElevatorState.EMERGENCY` + `triggerEmergency()`/`resolveEmergency()` |
| Thread safe | **Fixed** — per-elevator lock, see below |

If you list a requirement in your own doc, walk it line by line before calling the
design done. Anything not covered gets cut explicitly ("out of scope, here's why") or built.

## Correctness bugs found

- ~~`Elevator.java:64` — `requests.contains(elevatorRequest)` uses default reference
  equality~~ **Fixed** — `ElevatorRequest.equals()`/`hashCode()` now compare
  floor/elevatorId/direction/isInternalCommand, so the dedup check actually dedups.
- ~~`Elevator.java:67` — direction is only set in `addRequest` when state is
  `STOPPED`~~ Still true in isolation, but no longer causes a stale scheduling
  decision — `SchedulingScan`/`SchedulingFifo` now run under `Elevator.step()`'s lock
  alongside the actual move, so direction is always current by the time the elevator
  moves (see "Closing the last atomicity gap" below).
- ~~`ElevatorManager.step()` dispatches to `MAINTENANCE` elevators the same as any
  other~~ **Fixed** — `Elevator.step()`/`moveToNextFloor()` both check
  `isOutOfService()` (`MAINTENANCE` or `EMERGENCY`) and skip; `requestElevator`/
  `requestFloor` also refuse new requests to an out-of-service elevator at dispatch
  time, so requests no longer pile up against a car that will never process them.
- ~~`ElevatorManager.getElevators()`/`getFloors()` return the live mutable list~~
  **Fixed** — both now return `Collections.unmodifiableList(...)`.
- `Building` stores `totalElevator`/`totalFloors` as separate ints alongside lists that
  already have a size — two sources of truth that can diverge. **Not fixed** (out of
  scope of this pass — `Building` wasn't touched).
- `ElevatorState.IDEAL` (typo for IDLE), `Directions.IDLE` unused — small, but sloppy
  naming reads as low attention to detail under review. **Not fixed** — cosmetic,
  left as-is to avoid an unrelated rename touching every file that references it.

---

# Making it thread-safe

## Why it's currently unsafe

Two kinds of callers touch the same `Elevator` concurrently in any real deployment:
request-issuing threads (hall/car button presses — e.g. HTTP handlers) and whatever
drives `ElevatorManager.step()` (a scheduler thread or timer). Right now nothing
coordinates them:

- `Elevator.requests` is a plain `Queue`, not thread-safe for concurrent add/remove/peek.
- `currentFloor`, `elevatorState`, `directions` are plain fields — no `volatile`, no lock,
  so one thread's write may never become visible to another (a JMM visibility bug, not
  just a race), and multi-field transitions aren't atomic.
- `Elevator.addRequest()` is a compound operation: check-contains → add → read state →
  decide direction. If two threads call it concurrently, both can pass the `contains`
  check before either adds, or one can read `elevatorState` mid-transition set by the
  other. **A thread-safe queue alone would not fix this** — the bug is in the multi-step
  invariant across `requests` + `elevatorState` + `directions`, not in the queue type.
- `Elevator.moveToNextFloor()` → `completeArrival()` reads/writes `currentFloor`,
  `elevatorState`, and `requests` across several statements. If a `requestFloor` call
  lands on another thread between the `removeIf` and the `isEmpty()` check in
  `completeArrival`, the elevator can be marked `IDEAL` right after a live request was
  added — the new request is now stranded until the next unrelated event wakes it.

## Worked examples — what actually breaks

There's no wiring/driver code today that constructs `Elevator` objects, so `requests`
gets whatever `Queue` a caller passes in — almost certainly `LinkedList`, the default
reach for `Queue`. That matters for Example 1.

### Example 1 — the queue itself isn't safe for concurrent writers

`Elevator.addRequest()` (`Elevator.java:63-74`) does `requests.add(...)`. If two threads
call `requestFloor`/`requestElevator` for the same elevator at the same moment (two
riders press buttons within microseconds of each other — completely normal), you get
two concurrent `LinkedList.add()` calls:

```
Thread A: requests.add(reqFor7)   // mutating internal node pointers
Thread B: requests.add(reqFor2)   // mutating internal node pointers, same time
```

`LinkedList` isn't synchronized. Two concurrent structural mutations can corrupt the
internal `next`/`prev` pointers — the classic outcome is a **silently dropped element**
(one add "wins" and the other's node never gets linked in) or, less often, an infinite
loop if `step()` is iterating the same list at that moment. This is data-structure
corruption, not just a business-logic bug — the JDK docs say plainly `LinkedList` is
not thread-safe.

**Fix:** don't rely on the collection being safe by itself — guard every access with
the elevator's own lock (Option A below), or swap to `ConcurrentLinkedQueue`. The lock
is still needed regardless, because Example 2 requires it too.

### Example 2 — the real bug: `completeArrival()` racing `addRequest()` corrupts state

This is the one that matters most, because it silently produces a *wrong* state, not a
crash.

```java
private void completeArrival(){
    setElevatorState(ElevatorState.STOPPED);
    requests.removeIf(r -> r.getFloor() == currentFloor);
    if (requests.isEmpty()) {
        setElevatorState(ElevatorState.IDEAL);   // <-- line 99
    } else {
        setElevatorState(ElevatorState.RUNNING);
    }
}
```

Say elevator #3 just arrived at floor 5, its last request. Timeline with a second
thread handling an incoming request for floor 7 on the *same elevator*:

| Step | Thread S (stepper — inside `moveToNextFloor` → `completeArrival`) | Thread R (handling `requestFloor(7, 3)`) |
|---|---|---|
| 1 | `setElevatorState(STOPPED)` | |
| 2 | `requests.removeIf(...)` → queue now `[]` | |
| 3 | evaluates `requests.isEmpty()` → **true** (about to take the IDEAL branch) | |
| 4 | *(context switch)* | `addRequest(reqFor7)` runs: `requests.add(reqFor7)` → queue `[7]`; checks `elevatorState == STOPPED` → **still true** (Thread S hasn't written IDEAL yet) → sets `directions = UP` |
| 5 | resumes, already inside the `if` branch decided at step 3 → `setElevatorState(IDEAL)` | |

**End state:** `elevatorState = IDEAL`, `requests = [7]`, `directions = UP`. The
elevator now claims to be idle while it's actually holding a live, undispatched
request.

This directly breaks the design in `elevator-selection-strategy-suggestion.md`, which
says: *"Prefer an `IDEAL`/`STOPPED` elevator with the smallest `abs(currentFloor -
floor)`."* Once that selection strategy is wired up, it will see this elevator as
`IDEAL`, route a brand-new hall call to it, and now elevator #3 has two riders' requests
merged onto a car the system thinks is empty and available — invisible in a demo,
shows up under load.

**Fix:** the whole "stop, prune, decide next state" sequence has to execute as one
atomic unit relative to `addRequest`. Wrap both in the same lock:

```java
public class Elevator {
    private final Object lock = new Object();

    public void addRequest(ElevatorRequest r) {
        synchronized (lock) {
            if (!requests.contains(r)) requests.add(r);
            if (r != null && elevatorState == ElevatorState.STOPPED) {
                directions = r.getFloor() > currentFloor ? Directions.UP : Directions.DOWN;
            }
        }
    }

    private void completeArrival() {
        synchronized (lock) {
            elevatorState = ElevatorState.STOPPED;
            requests.removeIf(r -> r.getFloor() == currentFloor);
            elevatorState = requests.isEmpty() ? ElevatorState.IDEAL : ElevatorState.RUNNING;
        }
    }
}
```

With both under the same lock, Thread R's `addRequest` either runs entirely before
`completeArrival` starts (elevator correctly ends up `RUNNING`, floor-7 request seen)
or entirely after it finishes (elevator briefly is `IDEAL`, then `addRequest` flips it
back appropriately) — never interleaved in the middle. `moveToNextFloor` needs the same
lock around its whole body too, since it calls `completeArrival` internally and also
reads/writes `currentFloor`/`directions` beforehand.

### Example 3 — stale reads with no `volatile`, no lock (visibility, not just ordering)

`ElevatorManager.requestFloor` reads `elevator.getCurrentFloor()` from whatever thread
is handling the request:

```java
Directions directions = floor > elevator.getCurrentFloor() ? Directions.UP : Directions.DOWN;  // ElevatorManager.java:54
```

`currentFloor` is a plain `int` field, written by the stepper thread inside
`moveToNextFloor` (`Elevator.java:82-89`). Per the Java Memory Model, without a
`volatile` field or a lock, there is **no happens-before edge** between the stepper
thread's write and the request thread's read. This isn't hypothetical worst-case
pedantry — it's specified undefined behavior: the JIT is allowed to cache the field in
a register/hoist the read, and the request-handling thread can legally keep observing a
floor value from several ticks ago, computing the wrong `UP`/`DOWN` direction for a
request that then gets scheduled backwards.

**Fix:** once `currentFloor` reads/writes go through the same `synchronized(lock)`
block as Example 2 (either make the getter itself `synchronized`, or only ever read it
inside a locked section), the JMM guarantees visibility as a side effect of the lock —
no separate `volatile` needed once every access is lock-guarded. `volatile int
currentFloor` alone would fix visibility for *this* single-field read, but it would
**not** fix Example 2 (multi-field compound transition) — that's the Option C trap
below. Don't reach for `volatile` as a substitute for the lock; use both only if you
specifically need fast concurrent reads outside the locked path.

All three trace back to one root cause: `Elevator` has multiple threads touching
`requests` + `elevatorState` + `directions` + `currentFloor` with zero coordination.
One lock wrapped around every method that reads-or-writes more than one of those
fields fixes all three at once.

## What the rider actually experiences

Race conditions are easy to wave off as "an edge case" until you translate them into
what a person standing at a hall button sees. Same three examples, same root causes,
now as building-facing symptoms:

| Race | What a rider sees | Why |
|---|---|---|
| Example 1 — `LinkedList` corruption | Rider on floor 9 presses "down." Nothing ever comes. They give up, walk to the stairs, or press it again five minutes later and it works. No error anywhere — the request was silently dropped mid-`add()` by a concurrent write from another rider's button press elsewhere in the building. This is the "ghost call" complaint real building managers actually get. | Two `add()` calls into the same non-thread-safe `LinkedList` at once can corrupt its internal pointers and lose one of the two nodes being inserted. |
| Example 2 — `completeArrival()`/`addRequest()` race | Rider on floor 2 presses "up." The system skips elevator #1, which is sitting empty and idle two floors away, and instead sends elevator #3 all the way from the 20th floor — a 90-second wait instead of a 10-second one. Once elevator #3 arrives, it also stops at floor 7 for a *different* rider's request the new rider never asked for, adding another delay to their trip. | Elevator #3 was really still busy (had a pending floor-7 request) but its `elevatorState` field got corrupted to `IDEAL` mid-transition, so `ElevatorSelectionStrategy`'s "prefer the nearest idle car" logic (`elevator-selection-strategy-suggestion.md`) picks it over the genuinely idle elevator #1. |
| Example 3 — stale `currentFloor` read | Rider steps into a car already moving up past floor 6 and presses "5." The system computes `5 > currentFloor` using a stale cached value (e.g. `3`, from several ticks ago), decides `UP` is correct, and queues floor 5 as a stop the car is still "approaching" — except the car already physically passed floor 5. The elevator sails past every floor going up, the rider's floor never lights up as next, and they end up needing to press it again once the car turns around. | No happens-before edge between the stepper thread's write to `currentFloor` and the request-handling thread's read means the read can observe an arbitrarily stale value, so the UP/DOWN decision is computed against the wrong physical position of the car. |

None of these produce a crash or a stack trace — that's what makes them dangerous.
They show up as "the elevator felt dumb" or "it skipped my floor" complaints weeks
after ship, with nothing in the logs pointing at a race condition, because nothing
threw. This is the strongest reason to fix Examples 1–3 with real locking rather than
leaving them as "probably fine, we'll see it in testing" — these bugs are specifically
the kind that pass a single-threaded demo and only appear under real concurrent load,
i.e. exactly the conditions of a production building with more than one rider.

## Option A — coarse-grained lock per `Elevator` (fastest to implement)

Give every state-mutating method on `Elevator` the same lock, and make it intrinsic
(`synchronized`) or an explicit `ReentrantLock` field:

```java
public class Elevator {
    private final Object lock = new Object();
    ...
    public void addRequest(ElevatorRequest r) {
        synchronized (lock) {
            if (!requests.contains(r)) requests.add(r);
            if (r != null && elevatorState == ElevatorState.STOPPED) {
                directions = r.getFloor() > currentFloor ? Directions.UP : Directions.DOWN;
            }
        }
    }

    public void moveToNextFloor(int nextFloor) {
        synchronized (lock) {
            // existing body — the whole read-decide-write-completeArrival
            // sequence now happens as one atomic unit
        }
    }
}
```

Lock **per elevator**, not one global lock on `ElevatorManager` — elevators are
independent, so a manager-wide lock would serialize all cars against each other for no
reason. `ElevatorManager.step()` then just iterates and calls into each elevator's own
locked methods; no lock needed at the manager level unless `elevators`/`floors` become
mutable at runtime (add/remove elevator), in which case wrap those lists in
`CopyOnWriteArrayList` — reads (iteration in `step()`) vastly outnumber writes (adding
a new elevator).

This is the answer to give first in an interview if time is short: correct, easy to
reason about, easy to explain in one sentence ("each car owns its own lock; the manager
never needs a lock because it never mutates shared state directly").

## Option B — actor-per-elevator, using the Command pattern you already built

You already have `ElevatorRequest implements Command`. Instead of calling
`.execute()` synchronously from whatever thread received the request, give each
`Elevator` a single-consumer queue and a dedicated worker:

```java
public class Elevator {
    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

    public void submit(Command command) {
        commandQueue.offer(command);   // any thread may call this — queue is the only shared boundary
    }

    void runLoop() {                    // owned by exactly one worker thread per elevator
        while (running) {
            Command c = commandQueue.take();
            c.execute();                 // all state mutation happens on this one thread
        }
    }
}
```

`ElevatorManager.requestElevator`/`requestFloor` become `elevator.submit(new
ElevatorRequest(...))` instead of `dispatch(command)` calling `execute()` directly.
`step()`'s per-tick "move one floor" logic becomes another `Command` submitted to the
same queue, so movement and new requests interleave safely in submission order with
**zero locks anywhere** — no thread other than the elevator's own worker ever touches
its fields, so there's nothing to race. `ElevatorManager` itself only needs a
thread-safe collection of elevators if it supports adding/removing cars at runtime.

This is the more "SDE3-flavored" answer because it doesn't bolt synchronization onto
existing methods — it reuses a pattern you already designed (Command) and turns it into
the concurrency mechanism itself ("don't share memory, communicate" instead of "share
memory, guard it"). Worth proposing this as the target design and Option A as the
pragmatic first cut if you're short on time.

## Update — `ElevatorSelectionStrategy` implemented

`elevator-selection-strategy-suggestion.md`'s proposal is now built: `ElevatorController.requestElevator`
dropped `elevatorId` (hall call — system picks the car), `requestFloor` keeps it (car
call — rider is already inside a specific car). `NearestIdleSelectionStrategy` picks
nearest idle/stopped car first, then nearest on-the-way car, then nearest car
regardless of state, skipping `MAINTENANCE`/`EMERGENCY` throughout — injected into
`ElevatorManager` the same way `SchedulingStrategy` already was.

Worth naming honestly: `selectElevator` reads each candidate's state and floor one
elevator at a time via already-synchronized getters, not as one atomic snapshot across
every elevator. Under concurrent load, the elevator it picks could change state a
moment after being chosen (e.g. go into `MAINTENANCE` right after selection but before
dispatch). That's an accepted trade-off, not a bug: making the whole *n*-elevator scan
atomic would mean locking every elevator in the building for the duration of every hall
call, which is worse for throughput than occasionally picking a slightly suboptimal
car. The one invariant that must hold is that a stale pick can never corrupt state —
and it can't, because `addRequest`/`step` on the receiving elevator are still fully
locked regardless of how it was chosen.

`HallPanel` (`services/HallPanel.java`) is also now built: bound to one floor at
construction, `pressUp()`/`pressDown()` take no arguments — the floor is implicit in
which physical panel a rider is standing at, direction implicit in which button they
press. `ElevatorController.requestElevator(directions, floor)` itself still needs
`floor` internally (the manager has to know where to send the car); what changed is
that no *caller* of the rider-facing API states it anymore. `ElevatorPanel`
(`pressFloor`, car-bound) from the same doc is not yet built — natural next step, same
shape.

## Option C — atomics for individual fields (a trap to know about)

You might be tempted to make `currentFloor` an `AtomicInteger` and `elevatorState` an
`AtomicReference<ElevatorState>` and call it done. **This doesn't work here** — the bug
isn't in any single field, it's in the compound transition across `requests` +
`elevatorState` + `directions` + `currentFloor` together (see `moveToNextFloor` →
`completeArrival` above). Individually-atomic fields don't make a multi-field state
machine transition atomic; you'd still need a lock or a CAS loop around the whole
transition. Worth mentioning in an interview specifically because it's a common
half-right answer that sounds sophisticated but doesn't actually fix the bug class.

## How to prove it, not just claim it

Add a stress test once one of the above is in place:

```java
ExecutorService pool = Executors.newFixedThreadPool(16);
CountDownLatch start = new CountDownLatch(1);
for (int i = 0; i < 1000; i++) {
    int floor = ThreadLocalRandom.current().nextInt(1, totalFloors);
    pool.submit(() -> {
        try { start.await(); } catch (InterruptedException ignored) {}
        manager.requestFloor(floor, elevatorId);
    });
}
start.countDown();
pool.shutdown();
pool.awaitTermination(30, TimeUnit.SECONDS);
// then assert: no elevator's requests queue contains a floor it already
// passed and dropped, no elevator is stuck IDEAL with a non-empty queue,
// currentFloor is always within [1, totalFloors]
```

Fire many concurrent `requestElevator`/`requestFloor` calls alongside a stepping
thread, then assert invariants afterward (no lost requests, no elevator left `IDEAL`
with a non-empty queue, `currentFloor` never out of bounds). That's the artifact that
turns "I made it thread-safe" into something a reviewer can actually verify.

## Recommended path

1. Ship Option A first — it's a small, low-risk diff on the existing code and fixes the
   real bugs (lost requests, visibility) immediately.
2. If you want to demonstrate deeper design maturity, migrate to Option B — it removes
   locking entirely by construction and is a natural evolution of the Command pattern
   already in place.
3. Explicitly reject Option C in your explanation, and say why — it's the answer that
   sounds right and isn't, which is exactly the kind of distinction SDE3 interviewers
   are listening for.
