package com.lostark.tracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.tracker.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Emits the 401 (NOT 403) body for an unauthenticated {@code /api/admin/**} request directly, because
 * the security filter chain runs BEFORE the DispatcherServlet so {@code @RestControllerAdvice}
 * ({@code ApiExceptionHandler}) cannot catch it (D-02). The body is the SAME
 * {@code {timestamp,status,error,message}} {@link ApiErrorResponse} shape as every other 4xx,
 * serialized via the Spring-provided {@link ObjectMapper} (JavaTimeModule -> UTC ISO-8601). The
 * message is generic; the secret or header value is NEVER echoed or logged.
 */
@Component
public class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public AdminAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Missing or invalid admin secret");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
