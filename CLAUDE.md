# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Java solutions to classic Low-Level Design (LLD) interview problems. Each problem lives in
its own package under `src/`, generally with:

- a `problem.md` (or `Problem.md`) — the design brief (functional/non-functional requirements,
  core entities, chosen patterns) written *before* implementation,
- a runnable `Simulation`/`Main` class,
- and, where a review pass has happened, an `sde3-lld-review*.md` documenting bugs found and
  fixed at an SDE3 bar — read these before touching that package, they record *why* the code
  looks the way it does.

There is no build tool (no Maven/Gradle) — plain `javac`/`java` against the `src/` root, or the
IntelliJ project (`mechinecode.iml`).

## Build & run commands

Compile and run one package at a time, output goes to `out/` (gitignored):

```bash
# Elevator system
javac -d out $(find src/elevatordesign -name "*.java")
java -cp out elevatordesign.Simulation

# Logging system
javac -d out $(find src/loggingsystem -name "*.java")
java -cp out loggingsystem.Main

# Social media platform
javac -d out $(find src/socialmedia -name "*.java")
java -cp out socialmedia.Simulation

# Rate limiter
javac -d out $(find src/ratelimiter -name "*.java")
java -cp out ratelimiter.Main   # or exercise RateLimiter impls directly, no Main yet
```

There are no automated tests in this repo (no JUnit/test directory) — correctness is verified by
running the `Simulation`/`Main` class for the package and by the manual SDE3-review passes
documented in the `sde3-lld-review*.md` files. When adding non-trivial logic, prefer exercising
it through the package's `Simulation`/`Main` rather than adding a test framework unless asked.

## Packages

| Package | Problem | Key patterns |
|---|---|---|
| `src/elevatordesign` | Multi-elevator control system: hall/car calls, scheduling, maintenance/emergency modes | Command, Strategy (scheduling + car selection), Observer, per-elevator locking |
| `src/loggingsystem` | Logging framework with multiple levels and output destinations | Chain of handlers, Strategy (filters), Singleton |
| `src/socialmedia` | Social platform: profiles, follow graph, posts (text/media), likes, comments, DMs, feed | Facade, Factory, Strategy, Singleton |
| `src/ratelimiter` | Rate limiter with pluggable algorithms (fixed window implemented; token bucket in progress) | Strategy (`RateLimiter` interface + per-algorithm impl) |

### Social media platform architecture

`socialmedia.service.SocialMediaManager` is a Facade singleton in front of six
independently-singletoned subsystems:

- **`ProfileService`** — profile CRUD.
- **`FollowService`** — follow graph (`followingMap`/`followerMap`), including cascade cleanup
  on profile delete.
- **`service.post`** — `PostFactory` (Factory pattern) routes to `TextPostService`/
  `MediaPostService` by `PostType`, both extending the shared `PostService` for storage/ownership
  logic.
- **`LikeService`** / **`CommentService`** — per-post likes and comments, with duplicate-like
  and duplicate-comment guards and ownership-checked removal.
- **`MessageService`** — direct messages, indexed both per-user and per-conversation.
- **`FeedService`** — Strategy pattern (`FeedStrategy`); currently `RuntimeFeedStrategy`
  generates a feed on read by pulling posts from everyone a user follows, rather than fanning
  out on write (the tradeoff is discussed in `problem.md`).

Deletes cascade and this must be preserved when touching delete paths: `deletePost` also removes
that post's comments and likes; `deleteProfile` removes the user's own posts (which cascades
their comments/likes), any comments/likes they left on other posts, their follow edges in both
directions, and their conversations, before removing the profile itself.

See `src/socialmedia/sde3-lld-review.md` for bugs found and fixed (duplicate likes, missing
ownership checks, orphaned data on delete, mutable value objects used as map keys).

### Elevator system notes

See `src/elevatordesign/sde3-lld-review-and-thread-safety.md` for the concurrency bugs found in
the original single-threaded design (lost requests from a non-thread-safe queue, a
state-corrupting race between `completeArrival()` and `addRequest()`, stale reads with no
happens-before edge) and the two fixes considered: a coarse per-elevator lock (implemented) vs.
an actor-per-elevator model built on the existing Command pattern (proposed as the more
"SDE3-flavored" answer, not implemented). Any change to elevator state transitions should
preserve the per-elevator locking discipline described there.

### Rate limiter

`ratelimiter.service.RateLimiter` is the Strategy interface (`check(serviceName, operationName)`
throws `RateLimiterException` with a `retryAfterMillies` hint). `FixedWindowAlgo` keys counters
by `"serviceName:operationName"` in a `ConcurrentHashMap<String, AtomicInteger>` and resets all
counters on a fixed schedule via a single-threaded `ScheduledExecutorService`. `TokenBucketAlgo`
is a stub — not yet implemented.
