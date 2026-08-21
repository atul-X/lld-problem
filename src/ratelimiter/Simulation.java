package ratelimiter;

import ratelimiter.service.FixedWindowAlgo;
import ratelimiter.service.RateLimiter;
import ratelimiter.service.RateLimiterException;
import ratelimiter.service.TokenBucketAlgo;

public class Simulation {
    public static void main(String[] args) throws InterruptedException {
        runFixedWindow();
        System.out.println();
        runTokenBucket();
    }

    private static void runFixedWindow() throws InterruptedException {
        System.out.println("=== Fixed Window: limit=3 requests / 1000ms window ===");
        FixedWindowAlgo limiter = new FixedWindowAlgo(3, 1000);

        for (int i = 1; i <= 5; i++) {
            attempt(limiter, "orders", "create", i);
        }

        System.out.println("sleeping 1100ms for the window to reset...");
        Thread.sleep(1100);

        attempt(limiter, "orders", "create", 6);

        limiter.shutdown();
    }

    private static void runTokenBucket() throws InterruptedException {
        System.out.println("=== Token Bucket: capacity=3, refill=1 token/sec ===");
        TokenBucketAlgo limiter = new TokenBucketAlgo(3, 1.0);

        // burst: first 3 succeed immediately (full bucket), rest are rejected
        for (int i = 1; i <= 5; i++) {
            attempt(limiter, "orders", "create", i);
        }

        System.out.println("sleeping 2000ms so ~2 tokens refill...");
        Thread.sleep(2000);

        // bucket has refilled ~2 tokens: expect 2 allowed, then a rejection
        for (int i = 6; i <= 8; i++) {
            attempt(limiter, "orders", "create", i);
        }
    }

    private static void attempt(RateLimiter limiter, String service, String operation, int callNumber) {
        try {
            limiter.check(service, operation);
            System.out.println("call " + callNumber + ": allowed");
        } catch (RateLimiterException e) {
            System.out.println("call " + callNumber + ": rejected (" + e.getMessage()
                    + ", retry after " + e.getRetryAfterMillies() + "ms)");
        }
    }
}
