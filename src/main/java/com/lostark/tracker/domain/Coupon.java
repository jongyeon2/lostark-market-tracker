package com.lostark.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * An admin-registered coupon (code + reward + expiry). Mirrors {@link GameEvent}'s shape so the
 * admin CRUD and public read stacks reuse the same pattern (COUPON-01). {@code expiresAt} is a
 * date-only {@link LocalDate} (D-01) — no instant/timezone conversion — and validity is judged on
 * the server's KST date so the expiry day stays valid until its end (23:59 KST). {@code code} carries
 * no unique constraint (D-04): the same code may be re-registered freely.
 */
@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "reward", nullable = false, length = 300)
    private String reward;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Coupon() {
    }

    public Coupon(String code, String reward, LocalDate expiresAt) {
        this.code = code;
        this.reward = reward;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Full-replace mutation for {@code PUT /api/admin/coupons/{id}}: overwrites the three mutable
     * fields ({@code code}, {@code reward}, {@code expiresAt}). The timestamp columns are intentionally
     * untouched here: {@code @PreUpdate} stamps {@code updatedAt} on flush. No raw setters exist for the
     * timestamp fields.
     */
    public void replace(String code, String reward, LocalDate expiresAt) {
        this.code = code;
        this.reward = reward;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getReward() {
        return reward;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
