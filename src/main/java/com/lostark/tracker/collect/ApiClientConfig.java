package com.lostark.tracker.collect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires the productized {@link LostarkApiClient}. The builder is configured with per-call
 * connect/read timeouts (~5s — D-07: the per-call cap is what keeps one slow item from hanging
 * the whole tick). baseUrl + key come from {@code lostark.api.*} (env only — never committed).
 */
@Configuration
public class ApiClientConfig {

    /** Per-call timeout — conservative constant; externalization to properties is v2 (D-07). */
    private static final Duration PER_CALL_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient.Builder lostarkRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(PER_CALL_TIMEOUT);
        factory.setReadTimeout(PER_CALL_TIMEOUT);
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public LostarkApiClient lostarkApiClient(RestClient.Builder lostarkRestClientBuilder,
                                             @Value("${lostark.api.base-url}") String baseUrl,
                                             @Value("${lostark.api.key:}") String apiKey) {
        return new LostarkApiClient(lostarkRestClientBuilder, baseUrl, apiKey);
    }
}
