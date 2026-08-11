Design a Social Media Platform

Design a social media platform where users can create profiles,
follow each other, post updates, like and comment on posts, and send direct messages.
Consider scalability and the management of media content like images and videos.

Functional Requirements
    -profile creations && profile delete.
    -Post creations and deletions user can like and comment on post .
    -send messages to each other.
    -Feed service 
    -Follow and unfollow user 
Non-Functional Requirements
    -scalability
    -Concurrency
        on user post like count
    -extensibility
    -Correctness

Core Entities
    
    Profile
        -id
        -username
        -image
        -name
        -dob

    
    Follower
        -userid
        -followerId
        -timestamp
    
    Post
        -postId
        -userid
        -metadata
        -type
    
    metadata
        -fileName
        -s3url

    message
        -userid
        -reciverid
        -content
        -timestamp

    Comment
        -commentId
        -postId
        -userid
        -content

    Like
        -postId
        -userid
        -LikeId

SocialMediaManager will works a fecade here 
    ProfileService  
        design Pattern -> Singleton pattern
        Map<int,Profiles> profileMap=new HashMap<>();//userId,Profiles
        -Profile addProfile()
        -void deleteProfile();
        -Profile getUser();
    FollowService
        design Pattern -> Singleton pattern
        Map<Integer,Set<Integer>> followersMap=new HashMap<>();//followerId,FollowingId Mapping
        Map<Integer,Set<Integer>> followingsMap=new HashMap<>();//FollowingId,followerId Mapping
        -void followFriend(int follower,int follwingId);
        -void unfollowFriend();
    LikeService
        design Pattern -> Singleton pattern
        Map<Integer,Like> LikeMap=new ConcureentHashMap<>();// this will store likes against  like id
        Map<Integer,List<Integer>> LikeMap=new ConcureentHashMap<>();// this will store List of  like id  against  post id
        -Like addLikePost(int userid,int postid,Like like)
        -void removeLike(int userId,int LikeId)
    CommentService
        design Pattern -> Singleton pattern
        Map<Integer,List<Comment>> commentMap=new HashMap<>();// this will comments against  post id
        -PostService postService;
        -Comment addComment(int userid,int postid,Comment comment);
        -void removeComment(int userid,int postid,int commentId);
    PostService
        design Pattern -> Factory Design Pattern 
        -Map<Integer,Post> postMap=new HashMap;
        -Map<Integer,List<Integer>> postMap=new HashMap<>(); // this will store List of posts againts userId
        -Post addPost(int userId,Post post);
        -void deletePost(int postId,int user id)
        -List<Post> getUserPosts(user id));
        in this service for simplicity and lld perpouse will one use s3 urls and 
    
    MessageService
        Map<Integer,List<Message>> messageMap=new ConcurrentHashMap<>(); //this will store message againts userId
        Map<Integer,List<Message>> converstionMap=new ConcurrentHashMap<>(); //this will store message againts userId
        -Message sendMessage(Message);
        -List<Message> getConversion(int senderId,int reciverId)
        -List<Message> getAllConversion(int senderId);
    
    FeedService
        design Pattern -> Statergy pattern 
        1.two approach are we can create feed map for each user when he posts.
        2.In we can generate feed on runtime from FollowService and PostService
        -will do second in case of lld.
        -FollowService followService
        -PostService postService;
        -List<Post> getFeed();
