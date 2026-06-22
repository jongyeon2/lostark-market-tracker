package com.lostark.tracker.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Wires the self-built Redis token bucket (D-03/D-04).
 *
 * <p>Bucket parameters are constants with deliberate head-room under the measured 100/min limit
 * (Task 0): a single global bucket of 90 tokens refilling at 90/min. Externalising these to
 * {@code @ConfigurationProperties} is deferred to v2 per the phase context.
 */
@Configuration
public class RateLimiterConfig {

    /** Single global bucket — one API key, one bucket (D-03). */
    public static final String BUCKET_KEY = "lostark:ratelimit:tokens";

    /** Head-room under the real 100/min limit (D-04). */
    public static final long CAPACITY = 90;
    public static final double REFILL_PER_SEC = 90.0 / 60.0; // 90 tokens per minute

    @Bean
    public RedisScript<Long> tokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/token_bucket.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public RedisTokenBucket redisTokenBucket(StringRedisTemplate redis, RedisScript<Long> tokenBucketScript) {
        return new RedisTokenBucket(redis, tokenBucketScript, BUCKET_KEY, CAPACITY, REFILL_PER_SEC,
                System::currentTimeMillis);
    }
}
