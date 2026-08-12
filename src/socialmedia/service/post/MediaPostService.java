package socialmedia.service.post;

import socialmedia.model.Post;
import socialmedia.model.PostType;

public class MediaPostService extends PostService implements IPost{
    private static volatile MediaPostService instance;

    private MediaPostService() {
    }

    public static MediaPostService getInstance(){
        if (instance==null){
            synchronized (MediaPostService.class){
                if (instance==null){
                    instance=new MediaPostService();
                }
            }
        }
        return instance;
    }

    @Override
    public Post addPost(int userId, Post post) {
        if (post.getPostType() != PostType.MEDIA){
            throw new IllegalArgumentException("MediaPostService only supports MEDIA posts");
        }
        if (post.getUserId() != userId){
            throw new IllegalArgumentException("userId does not match post owner");
        }
        if (post.getMetadata() == null){
            throw new IllegalArgumentException("Media post requires metadata");
        }
        return addPost(post);
    }

    @Override
    public void deletePost(int postId, int profileId) {
        removePost(postId, profileId);
    }
}
