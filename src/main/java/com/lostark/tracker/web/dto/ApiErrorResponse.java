package com.lostark.tracker.web.dto;

import java.time.OffsetDateTime;

/**
 * The consistent 4xx error body shape (D-10): {@code {timestamp, status, error, message}}. Produced
 * by the custom {@code @RestControllerAdvice} so every client error asserts against one stable
 * contract. {@code timestamp} is UTC ISO-8601 ({@code ...Z}, D-11) like every other instant in the
 * API; {@code message} never carries a secret.
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message
) {
}
