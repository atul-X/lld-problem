package socialmedia.service.post;

import socialmedia.model.Post;

import java.util.List;

public interface IPost {
    Post addPost(int userId, Post post);
    void deletePost(int postId,int profileId);
    List<Post> getUserPosts(int userId);
}
