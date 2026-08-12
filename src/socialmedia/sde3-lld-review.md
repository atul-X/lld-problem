# SDE3 LLD Review — Social Media Platform

## Verdict: **Fixed** — was "Lean Hire for SDE2, borderline/no for SDE3"

Singleton, Factory, Strategy, and Facade are all applied correctly and for real
reasons, not decoration — that's the easy 60% of this problem and it's solid. What
originally held it back from a clean SDE3 bar was that the one NFR called out by name
in your own `problem.md` — *"Concurrency … on user post like count"* — wasn't
enforced, plus a handful of correctness/data-integrity gaps (ownership checks, cascade
deletes, mutable value objects). All six bugs below and every nit have since been fixed.

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
| Profile create/delete | Done — delete now cascades (see fix #3) |
| Post create/delete, like, comment | Done |
| Direct messages | Done (`MessageService`) |
| Feed service | Done — Strategy pattern, runtime generation as the doc chose |
| Follow/unfollow | Done |
| Concurrency on like count | **Fixed** — see fix #1 below |
| Extensibility | **Fixed** — `PostFactory`'s silent `default` case now fails loudly |
| Media content management | **Fixed** — `Metadata` now has a real constructor/getters |

## Bugs found

1. ~~**The flagship NFR isn't met — duplicate likes inflate the count.**
   `LikeService.addLike` had no check for "did this user already like this post."~~
   **Fixed** — `addLike` (`service/likes/LikeService.java:36`) is now `synchronized`
   and checks `likeIdsByPostId` for an existing like from the same user before
   inserting, throwing `IllegalStateException` on a duplicate.

2. ~~**No ownership checks on delete, inconsistently applied.**
   `CommentService.removeComment(commentId)` and `LikeService.removeLike(Like)` took
   no caller identity at all.~~ **Fixed** — both now require the requester's id and
   validate ownership before deleting: `CommentService.removeComment(commentId,
   profileId)` (`service/comment/CommentService.java:43`) and
   `LikeService.removeLike(likeId, requesterId)` (`service/likes/LikeService.java:53`),
   matching the pattern `PostService.removePost` already used.

3. ~~**No cascade delete → orphaned data.**
   `ProfileService.deleteProfile` only removed the profile map entry; posts, comments,
   likes, follow edges, and messages all stayed live. Same gap on `deletePost`.~~
   **Fixed** — `SocialMediaManager.deleteProfile` (`service/SocialMediaManager.java:61`)
   now deletes the user's own posts (cascading their comments/likes via
   `deletePost`), then purges any comments/likes they left on *other* posts
   (`CommentService.removeAllCommentsByProfile`, `LikeService.removeAllLikesByUser`),
   removes all follow edges in both directions (`FollowService.removeAllRelations`),
   and clears their conversations (`MessageService.removeAllMessagesForUser`), before
   removing the profile itself. `SocialMediaManager.deletePost` now also cleans up
   that post's comments and likes (`CommentService.removeAllCommentsForPost`,
   `LikeService.removeAllLikesForPost`).

4. ~~**Mutable value objects with setters that break map-key invariants.**
   `Post.setId()` let you rewrite a post's id after it was already a key in
   `postMap`; `Comment.setPostId`/`setProfileId` did the same for
   `commentMapByPostId`.~~ **Fixed** — `Post.id` and `Comment.postId`/`profileId` are
   now `final` with no setters, consistent with `Profile`, `Like`, and `Message`.

5. ~~**`Metadata` is a stub — media posts don't actually carry media.**
   No constructor, no getters/setters.~~ **Fixed** — `Metadata` now takes
   `s3url`/`fileName`/`uploadedAt` in its constructor with matching getters
   (`model/Metadata.java`); `Simulation.java` passes real values when creating a
   media post.

6. ~~**Dead code.** `model/Follower.java` had no constructor or getters and was
   unused.~~ **Fixed** — deleted.

## Nits — also fixed

- ~~`service.follwer` package name typo.~~ **Fixed** — renamed to
  `service.follower`, all imports updated.
- ~~`PostFactory.getPostInstance` used `default: return MediaPostService...` instead
  of an explicit `case MEDIA:`, so a future `PostType` would silently misroute.~~
  **Fixed** — `MEDIA` is now its own case; anything else throws
  `IllegalArgumentException`.
- ~~`SocialMediaManager.deletePost` reached into `TextPostService.getInstance()`/
  `MediaPostService.getInstance()` directly instead of going through `PostFactory`.~~
  **Fixed** — added `PostFactory.findServiceForPost(postId)`; both `deletePost` and
  `likePost`'s existence check now go through the factory, and the manager no longer
  imports the concrete post-service classes.

## Bonus fix found while wiring the above

`SocialMediaManager.likePost` had a live bug unrelated to this review's original
findings: it validated the target post existed by checking `getUserPosts(likeRequest
.getUserId())` — the **liker's own posts**, not the target post's owner — so liking
anyone else's post (the normal case) always failed with "Post not present." Fixed by
using the new `PostFactory.findServiceForPost` existence check instead, which doesn't
care who owns the post.

## Verification

`Simulation.java` now demonstrates every fix end-to-end (duplicate-like rejection,
cascade delete on both `deletePost` and `deleteProfile`), and a standalone smoke test
covering 20+ assertions — including non-owner rejection on unlike/uncomment/delete,
and post-cascade verification after `deleteProfile` — passes in full.

## Bar-raiser framing

Architecture and pattern selection here was already above SDE2 bar. What was missing
was the SDE3 differentiator: proactively reasoning about invariants under concurrency
and lifecycle edge cases (delete cascades, duplicate actions, ownership) rather than
just the happy path. With #1–#3 fixed — like-dedup, consistent ownership checks,
cascade delete — this now clears SDE3.
