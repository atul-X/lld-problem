package socialmedia.service.post;

import socialmedia.model.Post;
import socialmedia.model.PostType;

public class TextPostService extends PostService implements IPost{
    private static volatile TextPostService instance;

    private TextPostService() {
    }

    public static TextPostService getInstance(){
        if (instance==null){
            synchronized (TextPostService.class){
                if (instance==null){
                    instance=new TextPostService();
                }
            }
        }
        return instance;
    }

    @Override
    public Post addPost(int userId, Post post) {
        if (post.getPostType() != PostType.MESSAGE){
            throw new IllegalArgumentException("TextPostService only supports MESSAGE posts");
        }
        if (post.getUserId() != userId){
            throw new IllegalArgumentException("userId does not match post owner");
        }
        return addPost(post);
    }

    @Override
    public void deletePost(int postId, int profileId) {
        removePost(postId, profileId);
    }
}
