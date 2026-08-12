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
import socialmedia.service.follower.FollowService;
import socialmedia.service.likes.LikeService;
import socialmedia.service.message.MessageService;
import socialmedia.service.post.IPost;
import socialmedia.service.post.PostFactory;
import socialmedia.service.profile.ProfileService;

import java.util.List;
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
        for (Post post : getUserPosts(profileId)){
            deletePost(post.getId(), profileId);
        }
        commentService.removeAllCommentsByProfile(profileId);
        likeService.removeAllLikesByUser(profileId);
        followService.removeAllRelations(profileId);
        messageService.removeAllMessagesForUser(profileId);
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
        IPost postService = postFactory.findServiceForPost(postId);
        if (postService == null){
            throw new IllegalStateException("Post not found: " + postId);
        }
        postService.deletePost(postId, profileId);
        commentService.removeAllCommentsForPost(postId);
        likeService.removeAllLikesForPost(postId);
    }

    public List<Post> getUserPosts(int userId){
        return postFactory.getUserPosts(userId);
    }

    // ---- Like ----
    public Like likePost(LikeRequest likeRequest){
        if (postFactory.findServiceForPost(likeRequest.getPostId()) == null){
            throw new IllegalStateException("Post not present");
        }
        return likeService.addLike(likeRequest);
    }

    public void unlikePost(int likeId, int userId){
        likeService.removeLike(likeId, userId);
    }

    public int getLikeCount(int postId){
        return likeService.getLikeCount(postId);
    }

    // ---- Comment ----
    public Comment addComment(CommentRequest commentRequest){
        return commentService.addComment(commentRequest);
    }

    public void removeComment(int commentId, int profileId){
        commentService.removeComment(commentId, profileId);
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
