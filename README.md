# LLD Problems

Java solutions to classic Low-Level Design (LLD) interview problems. Each problem
lives in its own package under `src/`, with a `problem.md` describing the
requirements and design as originally sketched out, a runnable `Simulation`/`Main`
class, and (where a review pass has happened) an `sde3-lld-review*.md` documenting
bugs found and fixed at an SDE3 bar.

## Problems

| Package | Problem | Key patterns |
|---|---|---|
| `src/elevatordesign` | Multi-elevator control system: hall/car calls, scheduling, maintenance/emergency modes | Command, Strategy (scheduling + car selection), Observer, per-elevator locking |
| `src/loggingsystem` | Logging framework with multiple levels and output destinations | Chain of handlers, Strategy (filters), Singleton |
| `src/socialmedia` | Social platform: profiles, follow graph, posts (text/media), likes, comments, DMs, feed | Facade, Factory, Strategy, Singleton |

Each package's `problem.md` is the design brief (functional/non-functional
requirements, core entities, chosen patterns) written before implementation.

## Running

No build tool — plain `javac`/`java` against the `src/` root. From the repo root:

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
```

Or open the project in IntelliJ IDEA (`mechinecode.iml` is the module file) and run
any `Simulation`/`Main` class directly.

## Social media platform — architecture

`socialmedia.service.SocialMediaManager` is a Facade singleton in front of six
independently-singletoned subsystems:

- **`ProfileService`** — profile CRUD.
- **`FollowService`** — follow graph (`followingMap`/`followerMap`), including
  cascade cleanup on profile delete.
- **`service.post`** — `PostFactory` (Factory pattern) routes to
  `TextPostService`/`MediaPostService` by `PostType`, both extending the shared
  `PostService` for storage/ownership logic.
- **`LikeService`** / **`CommentService`** — per-post likes and comments, with
  duplicate-like and duplicate-comment guards and ownership-checked removal.
- **`MessageService`** — direct messages, indexed both per-user and per-conversation.
- **`FeedService`** — Strategy pattern (`FeedStrategy`); currently
  `RuntimeFeedStrategy` generates a feed on read by pulling posts from everyone a
  user follows, rather than fanning out on write (the tradeoff is discussed in
  `problem.md`).

Deleting a post or a profile cascades: `deletePost` also removes that post's
comments and likes; `deleteProfile` removes the user's own posts (which cascades
their comments/likes), any comments/likes they left on other posts, their follow
edges in both directions, and their conversations, before removing the profile
itself.

See `src/socialmedia/sde3-lld-review.md` for the review history — bugs found
(duplicate likes, missing ownership checks, orphaned data on delete, mutable
value objects used as map keys) and how each was fixed and verified.

## Elevator system — notes

See `src/elevatordesign/sde3-lld-review-and-thread-safety.md` for a detailed
walkthrough of the concurrency bugs found in the original single-threaded design
(lost requests from a non-thread-safe queue, a state-corrupting race between
`completeArrival()` and `addRequest()`, and stale reads with no
happens-before edge) and the two fixes considered: a coarse per-elevator lock
(implemented) versus an actor-per-elevator model built on the existing Command
pattern (proposed as the more "SDE3-flavored" answer).
