package socialmedia.model;

public class CommentRequest {
    private int postId;
    private int profileId;
    private String content;

    public CommentRequest(String content, int profileId, int postId) {
        this.content = content;
        this.profileId = profileId;
        this.postId = postId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
