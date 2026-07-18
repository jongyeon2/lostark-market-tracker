package com.lostark.tracker.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires {@link CollectionHeartbeat} with a dedicated short-timeout {@link RestClient} (design spec §7).
 * Mirrors {@link com.lostark.tracker.collect.ApiClientConfig}: the request factory (connect/read timeout)
 * is configured here on the builder, and the component receives that builder — keeping the component
 * unit-testable with a mock-bound plain builder.
 *
 * <p>{@code monitoring.collection.ping-url} defaults to blank (application.yml), so the heartbeat is a
 * no-op everywhere except prod, where {@code COLLECTION_PING_URL} is injected from {@code .env.prod}.
 */
@Configuration
public class MonitoringConfig {

    /** A heartbeat ping must be quick and non-blocking — it rides on the tick, so keep it well under it. */
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public CollectionHeartbeat collectionHeartbeat(
            @Value("${monitoring.collection.ping-url:}") String pingUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(PING_TIMEOUT);
        factory.setReadTimeout(PING_TIMEOUT);
        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        return new CollectionHeartbeat(builder, pingUrl);
    }
}
