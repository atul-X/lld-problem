package socialmedia.service.feed;

import socialmedia.model.Post;

import java.util.List;

public interface FeedStrategy {
    List<Post> generateFeed(int userId);
}
