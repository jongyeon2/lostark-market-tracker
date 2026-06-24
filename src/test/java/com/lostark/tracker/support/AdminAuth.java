package com.lostark.tracker.support;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Test helper for the admin shared-secret header. Every admin integration request sends
 * {@code X-Admin-Secret}: in Wave 1 (04-01) Spring Security is absent so the header is inert and the
 * request reaches the controller; in Wave 2 (04-02) the same header authenticates against the gate —
 * so the 04-01 ITs survive the gate with ZERO retrofit. Reused by every admin IT and the 04-02
 * auth-gate IT.
 */
public final class AdminAuth {

    public static final String HEADER = "X-Admin-Secret";

    private AdminAuth() {
    }

    /** An {@link HttpEntity} carrying {@code body} as JSON plus the {@code X-Admin-Secret} header. */
    public static <T> HttpEntity<T> entity(T body, String secret) {
        return new HttpEntity<>(body, headers(secret));
    }

    /** A body-less {@link HttpEntity} carrying only the {@code X-Admin-Secret} header (GET/DELETE). */
    public static HttpEntity<Void> entity(String secret) {
        return new HttpEntity<>(headers(secret));
    }

    private static HttpHeaders headers(String secret) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER, secret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}