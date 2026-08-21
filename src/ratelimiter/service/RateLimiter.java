package ratelimiter.service;

public interface RateLimiter {
    void check(String serviceName,String operationName) throws RateLimiterException;
}
