package socialmedia.service.comment;

import socialmedia.model.Comment;
import socialmedia.model.CommentRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentService {
    private static volatile CommentService instance;
    private final Map<Integer, Comment> commentMap=new HashMap<>();
    private final Map<Integer, List<Comment>> commentMapByPostId=new HashMap<>();

    private CommentService() {
    }

    public static CommentService getCommentService(){
        if (instance==null){
            synchronized (CommentService.class){
                if (instance==null){
                    instance=new CommentService();
                }
            }
        }
        return instance;
    }

    public synchronized Comment addComment(CommentRequest commentRequest){
        boolean commentExits = commentMapByPostId.getOrDefault(commentRequest.getPostId(), Collections.emptyList())
                .stream().anyMatch(p -> p.getProfileId()==commentRequest.getProfileId());
        if (commentExits){
            throw new IllegalStateException("Profile already commented on this post");
        }
        Comment comment=new Comment(commentRequest.getPostId(), commentRequest.getProfileId(),commentRequest.getContent());
        commentMap.putIfAbsent(comment.getId(),comment);
        commentMapByPostId.computeIfAbsent(comment.getPostId(),id->new ArrayList<>()).add(comment);
        return comment;
    }

    public synchronized void removeComment(int  commentId){
        if (!commentMap.containsKey(commentId)){
            throw new IllegalStateException("Comment does not exist: " + commentId);
        }
        Comment comment=commentMap.get(commentId);
        commentMap.remove(commentId);
        commentMapByPostId.get(comment.getPostId()).remove(comment);
    }

    public synchronized List<Comment> getCommentsForPost(int postId){
        return new ArrayList<>(commentMapByPostId.getOrDefault(postId, Collections.emptyList()));
    }

}
