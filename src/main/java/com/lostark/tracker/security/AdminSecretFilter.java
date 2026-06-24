package com.lostark.tracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Hand-rolled shared-secret gate for {@code /api/admin/**} (D-01). For an admin path it reads the
 * {@code X-Admin-Secret} header and constant-time compares it ({@link MessageDigest#isEqual} on UTF-8
 * bytes) against the env-sourced secret; on a match it sets an authenticated token in the
 * {@link SecurityContextHolder} so {@code .authenticated()} passes, otherwise it leaves the context
 * empty and {@link AdminAuthenticationEntryPoint} produces the 401. A blank configured secret NEVER
 * authenticates (fail closed). The header/secret is never logged or echoed. Single shared secret, one
 * layer — no HTTP Basic, no user/role model (that is v2).
 */
@Component
public class AdminSecretFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/admin/";
    private static final String HEADER = "X-Admin-Secret";

    private final String configuredSecret;

    public AdminSecretFilter(@Value("${admin.api.secret:}") String configuredSecret) {
        this.configuredSecret = configuredSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith(ADMIN_PATH_PREFIX) && isValid(request.getHeader(HEADER))) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // Always continue — authorization + the entry point decide the 401 for admin paths.
        filterChain.doFilter(request, response);
    }

    /** Constant-time compare; a blank configured secret or a missing header never authenticates. */
    private boolean isValid(String presented) {
        if (!StringUtils.hasText(configuredSecret) || presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                configuredSecret.getBytes(StandardCharsets.UTF_8));
    }
}
