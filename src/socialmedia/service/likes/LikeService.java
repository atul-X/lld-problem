package socialmedia.service.likes;

import socialmedia.model.Like;
import socialmedia.model.LikeRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LikeService {
    private static volatile LikeService instance;

    private final Map<Integer, Like> likeMap = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> likeIdsByPostId = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicInteger> likeCountByPostId = new ConcurrentHashMap<>();

    private LikeService() {
    }

    public static LikeService getLikeService(){
        if (instance == null){
            synchronized (LikeService.class){
                if (instance == null){
                    instance = new LikeService();
                }
            }
        }
        return instance;
    }

    public synchronized Like addLike(LikeRequest likeRequest){
        int postId = likeRequest.getPostId();
        int userId = likeRequest.getUserId();
        boolean alreadyLiked = likeIdsByPostId.getOrDefault(postId, Collections.emptySet()).stream()
                .anyMatch(likeId -> likeMap.get(likeId).getUserId() == userId);
        if (alreadyLiked){
            throw new IllegalStateException("User " + userId + " already liked post " + postId);
        }
        Like like = new Like(postId, userId);
        likeMap.put(like.getLikeId(), like);
        likeIdsByPostId.computeIfAbsent(postId, id -> ConcurrentHashMap.newKeySet())
                .add(like.getLikeId());
        likeCountByPostId.computeIfAbsent(postId, id -> new AtomicInteger(0))
                .incrementAndGet();
        return like;
    }

    public synchronized void removeLike(int likeId, int requesterId){
        Like like = likeMap.get(likeId);
        if (like == null){
            throw new IllegalStateException("Like does not exist: " + likeId);
        }
        if (like.getUserId() != requesterId){
            throw new IllegalStateException("User does not own this like");
        }
        likeMap.remove(likeId);
        Set<Integer> likeIds = likeIdsByPostId.get(like.getPostId());
        if (likeIds != null){
            likeIds.remove(likeId);
        }
        AtomicInteger count = likeCountByPostId.get(like.getPostId());
        if (count != null){
            count.decrementAndGet();
        }
    }

    public synchronized void removeAllLikesByUser(int userId){
        List<Like> toRemove = likeMap.values().stream()
                .filter(like -> like.getUserId() == userId)
                .collect(Collectors.toList());
        for (Like like : toRemove){
            removeLike(like.getLikeId(), userId);
        }
    }

    public synchronized void removeAllLikesForPost(int postId){
        Set<Integer> likeIds = likeIdsByPostId.remove(postId);
        if (likeIds == null){
            return;
        }
        for (int likeId : likeIds){
            likeMap.remove(likeId);
        }
        likeCountByPostId.remove(postId);
    }

    public int getLikeCount(int postId){
        AtomicInteger count = likeCountByPostId.get(postId);
        return count == null ? 0 : count.get();
    }
}
