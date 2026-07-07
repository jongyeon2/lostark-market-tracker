package com.lostark.tracker.web.dto;

import com.lostark.tracker.domain.Coupon;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response body for a coupon. Exposes the entity-stamped {@code createdAt}/{@code updatedAt} alongside
 * the mutable fields so callers can confirm the timestamps were set by the entity, never by the
 * controller/service. {@code expiresAt} serializes as a date-only {@code "YYYY-MM-DD"} string (D-01);
 * the two instants are UTC ISO-8601 ({@code ...Z}).
 */
public record CouponResponse(
        Long id,
        String code,
        String reward,
        LocalDate expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getReward(),
                coupon.getExpiresAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt());
    }
}
