package com.lostark.tracker.cache;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.lostark.tracker.web.dto.LatestPriceResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Wires the latest-price cache's value-serializing {@link RedisTemplate} (D-03).
 *
 * <p>This is a SEPARATE, distinct bean from the token bucket's {@code StringRedisTemplate}
 * ({@code RateLimiterConfig}) — it shares the same {@link RedisConnectionFactory} (one Redis,
 * one connection pool) but serializes {@link LatestPriceResponse} values as JSON rather than raw
 * strings. The cache layer is hand-rolled (explicit get/put/evict in {@link LatestPriceCache}),
 * mirroring the self-built token bucket: Spring's annotation-driven cache abstraction is
 * deliberately NOT used so the read-fill and the write-invalidation are both visible and
 * explainable (interview signal, D-03).
 */
@Configuration
public class CacheConfig {

    /**
     * JSON value serializer for {@link LatestPriceResponse}. The {@link ObjectMapper} registers
     * {@link JavaTimeModule} (so {@code collectedAt} stays ISO-8601, not an epoch — D-11) and
     * {@link ParameterNamesModule} (so the record's canonical constructor deserializes), with
     * {@code WRITE_DATES_AS_TIMESTAMPS} disabled so the cached instant round-trips as {@code ...Z}.
     */
    @Bean
    public RedisTemplate<String, LatestPriceResponse> latestPriceRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        var objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new ParameterNamesModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        var valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, LatestPriceResponse.class);

        RedisTemplate<String, LatestPriceResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
