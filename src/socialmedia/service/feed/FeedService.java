package socialmedia.service.feed;

import socialmedia.model.Post;

import java.util.List;

public class FeedService {
    private static volatile FeedService instance;

    private final FeedStrategy feedStrategy;

    private FeedService() {
        this.feedStrategy = new RuntimeFeedStrategy();
    }

    public static FeedService getFeedService(){
        if (instance == null){
            synchronized (FeedService.class){
                if (instance == null){
                    instance = new FeedService();
                }
            }
        }
        return instance;
    }

    public List<Post> getFeed(int userId){
        return feedStrategy.generateFeed(userId);
    }
}
