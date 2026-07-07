package com.lostark.tracker.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for creating ({@code POST}) and full-replacing ({@code PUT}) a coupon (COUPON-01).
 * Bound instead of the {@link com.lostark.tracker.domain.Coupon} entity so {@code id}/{@code createdAt}/
 * {@code updatedAt} cannot be mass-assigned from the body. All three fields are required; a
 * {@code @Valid} violation returns 400 on the shared error contract. {@code expiresAt} is a date-only
 * {@link LocalDate} (D-01) — deserialized from a {@code "YYYY-MM-DD"} JSON string.
 */
public record CouponRequest(
        @NotBlank String code,
        @NotBlank String reward,
        @NotNull LocalDate expiresAt
) {
}
