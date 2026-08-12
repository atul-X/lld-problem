package socialmedia.service.feed;

import socialmedia.model.Post;
import socialmedia.service.follower.FollowService;
import socialmedia.service.post.PostFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates a feed on-demand by pulling posts from everyone a user follows,
 * rather than maintaining a precomputed per-user feed.
 */
public class RuntimeFeedStrategy implements FeedStrategy {
    private final FollowService followService = FollowService.getFollowService();
    private final PostFactory postFactory = new PostFactory();

    @Override
    public List<Post> generateFeed(int userId) {
        List<Post> feed = new ArrayList<>();
        for (int followedId : followService.getFollowing(userId)){
            feed.addAll(postFactory.getUserPosts(followedId));
        }
        feed.sort(Comparator.comparing(Post::getCreatedAt).reversed());
        return feed;
    }
}
