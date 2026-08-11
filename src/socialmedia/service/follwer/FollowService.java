package socialmedia.service.follwer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FollowService {
    private static volatile FollowService instance;

    // userId -> set of userIds that userId follows
    private final Map<Integer, Set<Integer>> followingMap = new ConcurrentHashMap<>();
    // userId -> set of userIds that follow userId
    private final Map<Integer, Set<Integer>> followerMap = new ConcurrentHashMap<>();

    private FollowService() {
    }

    public static FollowService getFollowService(){
        if (instance == null){
            synchronized (FollowService.class){
                if (instance == null){
                    instance = new FollowService();
                }
            }
        }
        return instance;
    }

    public void followFriend(int followerId,int followingId){
        followingMap.computeIfAbsent(followerId, id -> ConcurrentHashMap.newKeySet()).add(followingId);
        followerMap.computeIfAbsent(followingId, id -> ConcurrentHashMap.newKeySet()).add(followerId);
    }

    public void unFollowFriend(int followerId,int followingId){
        Set<Integer> following = followingMap.get(followerId);
        if (following == null || !following.contains(followingId)){
            throw new IllegalStateException("Does not follow each other: " + followerId + " -> " + followingId);
        }
        following.remove(followingId);
        Set<Integer> followers = followerMap.get(followingId);
        if (followers != null){
            followers.remove(followerId);
        }
    }

    public Set<Integer> getFollowers(int userId){
        return followerMap.getOrDefault(userId, Set.of());
    }

    public Set<Integer> getFollowing(int userId){
        return followingMap.getOrDefault(userId, Set.of());
    }
}
