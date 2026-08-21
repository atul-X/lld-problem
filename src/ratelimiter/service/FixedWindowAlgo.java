package ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowAlgo implements RateLimiter {

    private final int limit;
    private final int millAfterRefresh;
    private final Map<String, AtomicInteger> tokenBucketMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public FixedWindowAlgo(int limit, int millAfterRefresh) {
        this.limit = limit;
        this.millAfterRefresh = millAfterRefresh;
        scheduler.scheduleAtFixedRate(
                () -> tokenBucketMap.replaceAll((key, tokens) -> new AtomicInteger(limit)),
                millAfterRefresh, millAfterRefresh, TimeUnit.MILLISECONDS);
    }

    @Override
    public void check(String serviceName, String operationName) throws RateLimiterException {
        String key = serviceName + ":" + operationName;
        AtomicInteger tokens = tokenBucketMap.computeIfAbsent(key, k -> new AtomicInteger(limit));

        if (tokens.getAndUpdate(t -> t > 0 ? t - 1 : t) <= 0) {
            throw new RateLimiterException(
                    "Rate limit exceeded for " + key, millAfterRefresh);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
