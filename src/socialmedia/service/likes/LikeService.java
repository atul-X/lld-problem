package socialmedia.service.likes;

import socialmedia.model.Like;
import socialmedia.model.LikeRequest;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    public Like addLike(LikeRequest likeRequest){
        Like like = new Like(likeRequest.getPostId(), likeRequest.getUserId());
        likeMap.put(like.getLikeId(), like);
        likeIdsByPostId.computeIfAbsent(like.getPostId(), id -> ConcurrentHashMap.newKeySet())
                .add(like.getLikeId());
        likeCountByPostId.computeIfAbsent(like.getPostId(), id -> new AtomicInteger(0))
                .incrementAndGet();
        return like;
    }

    public void removeLike(Like like){
        if (!likeMap.containsKey(like.getLikeId())){
            throw new IllegalStateException("Like does not exist: " + like.getLikeId());
        }
        likeMap.remove(like.getLikeId());
        Set<Integer> likeIds = likeIdsByPostId.get(like.getPostId());
        if (likeIds != null){
            likeIds.remove(like.getLikeId());
        }
        AtomicInteger count = likeCountByPostId.get(like.getPostId());
        if (count != null){
            count.decrementAndGet();
        }
    }

    public int getLikeCount(int postId){
        AtomicInteger count = likeCountByPostId.get(postId);
        return count == null ? 0 : count.get();
    }
}
