# SDE3 LLD Review — Social Media Platform

## Verdict: Lean Hire for SDE2, borderline/no for SDE3 as-is

Singleton, Factory, Strategy, and Facade are all applied correctly and for real
reasons, not decoration — that's the easy 60% of this problem and it's solid. What
holds it back from a clean SDE3 bar is that the one NFR called out by name in your own
`problem.md` — *"Concurrency … on user post like count"* — isn't actually enforced,
plus a handful of correctness/data-integrity gaps (ownership checks, cascade deletes,
mutable value objects) that a bar-raiser treats as "did you think past the happy path."

## What's working

- **Facade** (`service/SocialMediaManager.java`) cleanly hides six subsystems
  (Profile, Follow, Post, Like, Comment, Message, Feed) behind one surface —
  `Simulation.java` never touches a concrete service directly.
- **Factory** (`service/post/PostFactory.java`) + **Strategy**
  (`service/feed/FeedStrategy.java` / `RuntimeFeedStrategy.java`) map exactly to what
  `problem.md` asked for, including the explicit "runtime vs precomputed feed"
  tradeoff the doc itself calls out.
- Every singleton (`ProfileService`, `FollowService`, `LikeService`, `CommentService`,
  `MessageService`, `TextPostService`, `MediaPostService`, `FeedService`,
  `SocialMediaManager`) uses correct double-checked locking: `private static volatile`
  field + `synchronized` block + null-check inside the block.
- `Post.id` is generated from a single shared `AtomicInteger` on the `Post` class
  itself, not per-service — so IDs stay globally unique across `TextPostService` and
  `MediaPostService`, and `SocialMediaManager.deletePost` can safely search both
  services without id-collision ambiguity. A detail a lot of candidates miss.
- Locking strategy is tailored per service rather than copy-pasted: `ConcurrentHashMap`
  + `AtomicInteger`/`newKeySet()` for `LikeService`/`FollowService` (independent
  per-key operations), vs. `HashMap` + `synchronized` methods for `ProfileService`/
  `CommentService`/`PostService`/`MessageService` (compound check-then-act operations
  that need a single lock). That's the right call to make per-service, not a
  one-size-fits-all default.

## Requirements coverage vs `problem.md`

| Stated requirement | Status |
|---|---|
| Profile create/delete | Done, but delete doesn't cascade (see bugs) |
| Post create/delete, like, comment | Done |
| Direct messages | Done (`MessageService`) |
| Feed service | Done — Strategy pattern, runtime generation as the doc chose |
| Follow/unfollow | Done |
| Concurrency on like count | **Not met** — see bug #1 below |
| Extensibility | Mostly — undercut by `PostFactory`'s silent `default` case (nit) |
| Media content management | **Not really met** — `Metadata` is an unusable stub |

## Bugs found

1. **The flagship NFR isn't met — duplicate likes inflate the count.**
   `LikeService.addLike` (`service/likes/LikeService.java:32`) has no check for
   "did this user already like this post." Call it twice and the count double-counts.
   `CommentService` enforces one-comment-per-user-per-post
   (`service/comment/CommentService.java:32-36`); `LikeService` enforces nothing on
   the exact metric the design doc singles out as the concurrency-critical one.

2. **No ownership checks on delete, inconsistently applied.**
   `PostService.removePost` validates `post.getUserId() == profileId`
   (`service/post/PostService.java:29-31`) before deleting — good. But
   `CommentService.removeComment(commentId)` (`service/comment/CommentService.java:43`)
   and `LikeService.removeLike(Like)` (`service/likes/LikeService.java:42`) take no
   caller identity at all — anyone who knows a `commentId` or holds a `Like` object can
   delete someone else's comment or like. Pick one authorization model and apply it
   everywhere, not just on posts.

3. **No cascade delete → orphaned data.**
   `ProfileService.deleteProfile` (`service/profile/ProfileService.java:44-49`) only
   removes the profile map entry. The deleted user's posts, comments, likes, follow
   edges, and messages all stay live, now pointing at a ghost user. Same gap on
   `deletePost` — its comments/likes are never cleaned up. This is exactly the kind of
   "walk me through delete" follow-up that comes up in review.

4. **Mutable value objects with setters that break map-key invariants.**
   `Post.setId()` (`model/Post.java:34`) lets you rewrite a post's id after it's
   already a key in `PostService.postMap`; `Comment.setPostId`/`setProfileId`
   (`model/Comment.java:24-26,32-34`) do the same for `commentMapByPostId`. Every other
   model (`Profile`, `Like`, `Message`) correctly made `id` `final` with no setter —
   `Post`/`Comment` are the outliers, and it's a live invariant-violation risk, not
   just a style nit.

5. **`Metadata` is a stub — media posts don't actually carry media.**
   No constructor, no getters/setters (`model/Metadata.java`). You can construct
   `new Metadata()` and satisfy the null-check in `MediaPostService.addPost`
   (`service/post/MediaPostService.java:31-33`), but `s3url`/`fileName`/`uploadedAt`
   can never be set or read. Given `problem.md` explicitly calls out "management of
   media content like images and videos," this requirement is currently
   unimplemented, not just rough around the edges.

6. **Dead code.** `model/Follower.java` has no constructor or getters and is unused —
   `FollowService` stores everything in raw `Map<Integer, Set<Integer>>` instead.
   Delete it or wire it in; leaving it is the kind of leftover that reads as an
   unfinished design pass.

## Smaller nits

- `service.follwer` package name typo — cosmetic, but the kind of thing that erodes
  "attention to detail" scoring, especially in a package name where fixing it later
  means a cross-file rename.
- `PostFactory.getPostInstance` (`service/post/PostFactory.java:12-18`) uses
  `default: return MediaPostService...` instead of an explicit `case MEDIA:`. Add a
  third `PostType` later (e.g. `POLL`, `STORY`) and it silently misroutes instead of
  failing loudly — undercuts the "extensibility" NFR listed in `problem.md`.
- `SocialMediaManager.deletePost` (`service/SocialMediaManager.java:94-101`) reaches
  into `TextPostService.getInstance()`/`MediaPostService.getInstance()` directly
  instead of going through `PostFactory`, inconsistent with how `createPost` uses the
  factory — leaks the concrete post-service classes into the facade and partially
  defeats the point of having the factory.

## Bar-raiser framing

Architecture and pattern selection here is above SDE2 bar. What's missing is the SDE3
differentiator: proactively reasoning about invariants under concurrency and lifecycle
edge cases (delete cascades, duplicate actions, ownership) rather than just the happy
path. Fixing bugs #1–#3 — like-dedup, consistent ownership checks, cascade delete —
would comfortably clear SDE3.
