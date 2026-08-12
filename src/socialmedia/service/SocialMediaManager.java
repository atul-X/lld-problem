package socialmedia.service;

import socialmedia.model.Comment;
import socialmedia.model.CommentRequest;
import socialmedia.model.Like;
import socialmedia.model.LikeRequest;
import socialmedia.model.Message;
import socialmedia.model.MessageRequest;
import socialmedia.model.Post;
import socialmedia.model.Profile;
import socialmedia.model.ProfileRequest;
import socialmedia.service.comment.CommentService;
import socialmedia.service.feed.FeedService;
import socialmedia.service.follwer.FollowService;
import socialmedia.service.likes.LikeService;
import socialmedia.service.message.MessageService;
import socialmedia.service.post.IPost;
import socialmedia.service.post.MediaPostService;
import socialmedia.service.post.PostFactory;
import socialmedia.service.post.TextPostService;
import socialmedia.service.profile.ProfileService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SocialMediaManager {
    private static volatile SocialMediaManager instance;

    private final ProfileService profileService;
    private final FollowService followService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final MessageService messageService;
    private final FeedService feedService;
    private final PostFactory postFactory;

    private SocialMediaManager() {
        this.profileService = ProfileService.getProfileService();
        this.followService = FollowService.getFollowService();
        this.likeService = LikeService.getLikeService();
        this.commentService = CommentService.getCommentService();
        this.messageService = MessageService.getMessageService();
        this.feedService = FeedService.getFeedService();
        this.postFactory = new PostFactory();
    }

    public static SocialMediaManager getInstance(){
        if (instance == null){
            synchronized (SocialMediaManager.class){
                if (instance == null){
                    instance = new SocialMediaManager();
                }
            }
        }
        return instance;
    }

    // ---- Profile ----
    public Profile createProfile(ProfileRequest profileRequest){
        return profileService.addProfile(profileRequest);
    }

    public void deleteProfile(int profileId){
        profileService.deleteProfile(profileId);
    }

    public Profile getProfile(int profileId){
        return profileService.getProfile(profileId);
    }

    // ---- Follow ----
    public void followUser(int followerId, int followingId){
        followService.followFriend(followerId, followingId);
    }

    public void unfollowUser(int followerId, int followingId){
        followService.unFollowFriend(followerId, followingId);
    }

    public Set<Integer> getFollowers(int userId){
        return followService.getFollowers(userId);
    }

    public Set<Integer> getFollowing(int userId){
        return followService.getFollowing(userId);
    }

    // ---- Post ----
    public Post createPost(int userId, Post post){
        IPost postService = postFactory.getPostInstance(post.getPostType());
        return postService.addPost(userId, post);
    }

    public void deletePost(int postId, int profileId){
        Post post = TextPostService.getInstance().getPost(postId);
        IPost postService = post != null ? TextPostService.getInstance() : MediaPostService.getInstance();
        if (post == null && MediaPostService.getInstance().getPost(postId) == null){
            throw new IllegalStateException("Post not found: " + postId);
        }
        postService.deletePost(postId, profileId);
    }

    public List<Post> getUserPosts(int userId){
        return postFactory.getUserPosts(userId);
    }

    // ---- Like ----
    public Like likePost(LikeRequest likeRequest){
        List<Post> postList=getUserPosts(likeRequest.getUserId());
        Optional<Post> post=postList.stream().filter(p->p.getId()==likeRequest.getPostId()).findFirst();
        if (!post.isPresent()){
            throw new IllegalStateException("Post not present");
        }
//        if(likeRequest.)
        return likeService.addLike(likeRequest);
    }

    public void unlikePost(Like like){
        likeService.removeLike(like);
    }

    public int getLikeCount(int postId){
        return likeService.getLikeCount(postId);
    }

    // ---- Comment ----
    public Comment addComment(CommentRequest commentRequest){
        return commentService.addComment(commentRequest);
    }

    public void removeComment(int commentId){
        commentService.removeComment(commentId);
    }

    public List<Comment> getComments(int postId){
        return commentService.getCommentsForPost(postId);
    }

    // ---- Feed ----
    public List<Post> getFeed(int userId){
        return feedService.getFeed(userId);
    }

    // ---- Message ----
    public Message sendMessage(MessageRequest messageRequest){
        return messageService.sendMessage(messageRequest);
    }

    public List<Message> getConversation(int senderId, int receiverId){
        return messageService.getConversation(senderId, receiverId);
    }

    public List<Message> getAllConversations(int userId){
        return messageService.getAllConversations(userId);
    }
}
