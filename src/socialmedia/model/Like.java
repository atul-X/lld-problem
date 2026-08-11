package socialmedia.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Like {
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    private final int likeId;
    private final int postId;
    private final int userId;

    public Like(int postId, int userId) {
        this.postId = postId;
        this.userId = userId;
        this.likeId = idGenerator.getAndIncrement();
    }

    public int getPostId() {
        return postId;
    }

    public int getUserId() {
        return userId;
    }

    public int getLikeId() {
        return likeId;
    }

}
