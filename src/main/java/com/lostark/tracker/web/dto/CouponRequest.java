package com.lostark.tracker.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for creating ({@code POST}) and full-replacing ({@code PUT}) a coupon (COUPON-01).
 * Bound instead of the {@link com.lostark.tracker.domain.Coupon} entity so {@code id}/{@code createdAt}/
 * {@code updatedAt} cannot be mass-assigned from the body. A {@code @Valid} violation returns 400 on
 * the shared error contract. Dates are date-only {@link LocalDate} (D-01) — deserialized from a
 * {@code "YYYY-MM-DD"} JSON string.
 *
 * <p>{@code startsAt} is OPTIONAL (V8): a coupon whose start date the admin does not know is registered
 * without one and renders as "~ 만료일" rather than carrying a fabricated date. Everything else is
 * required.
 */
public record CouponRequest(
        @NotBlank String code,
        @NotBlank String reward,
        LocalDate startsAt,
        @NotNull LocalDate expiresAt
) {

    /**
     * 기간의 순서 — a start after the expiry is not a coupon, it is a typo, and the panel renders the
     * pair as a range, so let it fail loudly at the boundary (400) instead of displaying nonsense.
     * Skipped when either side is absent: a null {@code startsAt} is legal, and a null
     * {@code expiresAt} is already {@code @NotNull}'s error to report.
     */
    @JsonIgnore
    @AssertTrue(message = "startsAt must not be after expiresAt")
    public boolean isPeriodOrdered() {
        return startsAt == null || expiresAt == null || !startsAt.isAfter(expiresAt);
    }
}
