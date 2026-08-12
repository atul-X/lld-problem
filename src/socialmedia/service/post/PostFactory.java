package socialmedia.service.post;

import socialmedia.model.Post;
import socialmedia.model.PostType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostFactory {

    public IPost getPostInstance(PostType postType){
        switch (postType){
            case MESSAGE:
                return TextPostService.getInstance();
            case MEDIA:
                return MediaPostService.getInstance();
            default:
                throw new IllegalArgumentException("Unsupported post type: " + postType);
        }
    }

    public List<Post> getUserPosts(int userId){
        List<Post> posts = new ArrayList<>();
        posts.addAll(TextPostService.getInstance().getUserPosts(userId));
        posts.addAll(MediaPostService.getInstance().getUserPosts(userId));
        posts.sort(Comparator.comparing(Post::getCreatedAt));
        return posts;
    }

    public IPost findServiceForPost(int postId){
        if (TextPostService.getInstance().getPost(postId) != null){
            return TextPostService.getInstance();
        }
        if (MediaPostService.getInstance().getPost(postId) != null){
            return MediaPostService.getInstance();
        }
        return null;
    }

}
