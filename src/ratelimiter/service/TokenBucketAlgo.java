package ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TokenBucketAlgo implements RateLimiter{
    private record Bucket(double token,long lastRefillNanos){}
    private final  int capacity;
    private final double refillTokensPerNano;
    private final Map<String,Bucket> buckets=new ConcurrentHashMap<>();

    public TokenBucketAlgo(int capacity, double refillTokensPerSecond) {
        this.capacity = capacity;
        this.refillTokensPerNano = refillTokensPerSecond/1_000_000_000;
    }

    @Override
    public void check(String serviceName, String operationName) throws RateLimiterException {
        String key=serviceName+":"+operationName;
        long now=System.nanoTime();
        RateLimiterException[] rejected=new RateLimiterException[1];
        buckets.compute(key,(k,existing)->{
            Bucket current=existing==null?new Bucket(capacity,now):existing;
            long elapsed=now-current.lastRefillNanos();
            double refreshed=Math.min(capacity, current.token()+elapsed*refillTokensPerNano);
            if (refreshed>=1){
                return new Bucket(refreshed-1,now);
            }else {
                long retryAfterNanos=(long) ((1-refreshed)/refillTokensPerNano);
                rejected[0] = new RateLimiterException(
                        "Rate limit exceeded for " + k,
                        (int)
                                TimeUnit.NANOSECONDS.toMillis(retryAfterNanos));
                return new Bucket(refreshed, now); // still save the refill progress
            }
        });
        if (rejected[0]!=null) throw rejected[0];

    }
}
