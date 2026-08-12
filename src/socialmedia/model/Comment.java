package socialmedia.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Comment {
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    private final int postId;
    private final int profileId;
    private String content;
    private final int id;

    public Comment(int postId, int profileId, String content) {
        this.postId = postId;
        this.profileId = profileId;
        this.content = content;
        this.id=idGenerator.getAndIncrement();
    }

    public int getPostId() {
        return postId;
    }

    public int getProfileId() {
        return profileId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getId() {
        return id;
    }

}
