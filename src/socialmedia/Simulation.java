package socialmedia;

import socialmedia.model.Comment;
import socialmedia.model.CommentRequest;
import socialmedia.model.Like;
import socialmedia.model.LikeRequest;
import socialmedia.model.Message;
import socialmedia.model.MessageRequest;
import socialmedia.model.Metadata;
import socialmedia.model.Post;
import socialmedia.model.PostType;
import socialmedia.model.Profile;
import socialmedia.model.ProfileRequest;
import socialmedia.service.SocialMediaManager;

import java.util.List;

public class Simulation {
    public static void main(String[] args) {
        SocialMediaManager manager = SocialMediaManager.getInstance();

        System.out.println("=== creating profiles ===");
        Profile alice = manager.createProfile(new ProfileRequest("alice", "Alice", "2000-01-01", "9990001"));
        Profile bob = manager.createProfile(new ProfileRequest("bob", "Bob", "1999-05-12", "9990002"));
        Profile carol = manager.createProfile(new ProfileRequest("carol", "Carol", "2001-09-23", "9990003"));
        System.out.println(alice.getUsername() + " -> id " + alice.getId());
        System.out.println(bob.getUsername() + " -> id " + bob.getId());
        System.out.println(carol.getUsername() + " -> id " + carol.getId());

        System.out.println("\n=== follow graph ===");
        manager.followUser(bob.getId(), alice.getId());
        manager.followUser(carol.getId(), alice.getId());
        manager.followUser(alice.getId(), bob.getId());
        System.out.println("bob & carol follow alice, alice follows bob");
        System.out.println("alice's followers: " + manager.getFollowers(alice.getId()));

        System.out.println("\n=== alice posts ===");
        Post textPost = manager.createPost(alice.getId(),
                new Post(alice.getId(), PostType.MESSAGE, null, "Hello world, this is my first post!"));
        System.out.println("text post id " + textPost.getId() + ": \"" + textPost.getContent() + "\"");

        Metadata metadata = new Metadata();
        Post mediaPost = manager.createPost(alice.getId(),
                new Post(alice.getId(), PostType.MEDIA, metadata, "Check out this photo"));
        System.out.println("media post id " + mediaPost.getId() + ": \"" + mediaPost.getContent() + "\"");

        System.out.println("\n=== bob likes and comments on alice's text post ===");
        Like like = manager.likePost(new LikeRequest(textPost.getId(), bob.getId()));
        System.out.println("like count on post " + textPost.getId() + ": " + manager.getLikeCount(textPost.getId()));

        Comment comment = manager.addComment(new CommentRequest("Nice post!", bob.getId(), textPost.getId()));
        System.out.println("comment by " + comment.getProfileId() + ": \"" + comment.getContent() + "\"");

        System.out.println("\n=== bob and carol's feeds (posts from people they follow) ===");
        printFeed("bob", manager.getFeed(bob.getId()));
        printFeed("carol", manager.getFeed(carol.getId()));

        System.out.println("\n=== direct messages ===");
        manager.sendMessage(new MessageRequest(bob.getId(), alice.getId(), "Loved your post!"));
        manager.sendMessage(new MessageRequest(alice.getId(), bob.getId(), "Thanks Bob!"));
        List<Message> conversation = manager.getConversation(alice.getId(), bob.getId());
        System.out.println("conversation between alice & bob (" + conversation.size() + " messages):");
        for (Message message : conversation) {
            System.out.println("  [" + message.getSentAt() + "] " + message.getSenderId()
                    + " -> " + message.getReceiverId() + ": " + message.getContent());
        }

        System.out.println("\n=== cleanup: unlike, remove comment, delete a post ===");
        manager.unlikePost(like);
        System.out.println("like count after unlike: " + manager.getLikeCount(textPost.getId()));
        manager.removeComment(comment.getId());
        System.out.println("comments after removal: " + manager.getComments(textPost.getId()).size());
        manager.deletePost(textPost.getId(), alice.getId());
        System.out.println("alice's posts after deleting the text post: " + manager.getUserPosts(alice.getId()).size());

        System.out.println("\n=== unfollow ===");
        manager.unfollowUser(carol.getId(), alice.getId());
        System.out.println("carol's feed after unfollowing alice: " + manager.getFeed(carol.getId()).size() + " posts");

        System.out.println("\n=== simulation finished ===");
    }

    private static void printFeed(String name, List<Post> feed) {
        System.out.println(name + "'s feed (" + feed.size() + " posts):");
        for (Post post : feed) {
            System.out.println("  post " + post.getId() + " [" + post.getPostType() + "] by user "
                    + post.getUserId() + ": \"" + post.getContent() + "\"");
        }
    }
}
