package com.lostark.tracker.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Single shared-secret security layer (D-01, D-02). Adding {@code spring-boot-starter-security} locks
 * every endpoint by default, so this chain EXPLICITLY permitAll's the Phase 1-3 public read surface
 * (GET {@code /api/items/**}, {@code /api/health/**}) and {@code /actuator/**} — only
 * {@code /api/admin/**} is {@code .authenticated()}, and {@code anyRequest()} stays permitAll so
 * nothing else regresses (the Core Value pipeline never depends on the admin secret). CSRF is off and
 * the session is STATELESS (header-authenticated JSON API). The hand-rolled {@link AdminSecretFilter}
 * populates the SecurityContext; {@link AdminAuthenticationEntryPoint} emits the 401 JSON contract.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AdminSecretFilter adminSecretFilter,
                                    AdminAuthenticationEntryPoint entryPoint) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/items/**", "/api/health/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .addFilterBefore(adminSecretFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * The {@code @Component} {@link AdminSecretFilter} would otherwise be auto-registered as a plain
     * servlet filter (outside the security chain) by Boot. Disable that registration so it runs ONLY
     * where {@code addFilterBefore} places it in the security chain.
     */
    @Bean
    FilterRegistrationBean<AdminSecretFilter> adminSecretFilterRegistration(AdminSecretFilter filter) {
        FilterRegistrationBean<AdminSecretFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}