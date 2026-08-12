package socialmedia.service.post;

import socialmedia.model.Post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PostService {
    protected final Map<Integer, Post> postMap=new HashMap<>();
    protected final Map<Integer, List<Integer>> postMapByProfileId=new HashMap<>();

    protected synchronized Post addPost(Post post){
        if (postMap.containsKey(post.getId())){
            throw new IllegalStateException("Post already exists");
        }
        postMap.putIfAbsent(post.getId(), post);
        postMapByProfileId.computeIfAbsent(post.getUserId(),id->new ArrayList<>()).add(post.getId());
        return post;
    }

    protected synchronized void removePost(int postId, int profileId){
        Post post = postMap.get(postId);
        if (post == null){
            throw new IllegalStateException("Post not found");
        }
        if (post.getUserId() != profileId){
            throw new IllegalStateException("User does not own this post");
        }
        postMap.remove(postId);
        postMapByProfileId.getOrDefault(post.getUserId(), Collections.emptyList())
                .remove(Integer.valueOf(postId));
    }

    public synchronized Post getPost(int postId){
        return postMap.get(postId);
    }

    public synchronized List<Post> getUserPosts(int userId){
        List<Integer> postIds = postMapByProfileId.getOrDefault(userId, Collections.emptyList());
        List<Post> posts = new ArrayList<>();
        for (int postId : postIds){
            posts.add(postMap.get(postId));
        }
        return posts;
    }
}
