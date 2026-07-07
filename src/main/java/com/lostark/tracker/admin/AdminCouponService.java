package com.lostark.tracker.admin;

import com.lostark.tracker.domain.Coupon;
import com.lostark.tracker.repository.CouponRepository;
import com.lostark.tracker.web.dto.CouponRequest;
import com.lostark.tracker.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin write/read service for coupons (COUPON-01), mirroring {@link AdminEventService}.
 * {@code create}/{@code replace} persist via the entity, so {@code created_at}/{@code updated_at} are
 * stamped by {@code @PrePersist}/{@code @PreUpdate} — never assigned here. {@code replace} is a FULL
 * replace of the three mutable fields. Missing ids on {@code replace}/{@code delete} raise
 * {@link ResourceNotFoundException} -> 404. {@code list} keeps expired coupons and sorts
 * soonest-expiry-first.
 */
@Service
public class AdminCouponService {

    private final CouponRepository couponRepository;

    public AdminCouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon create(CouponRequest request) {
        return couponRepository.save(new Coupon(
                request.code(), request.reward(), request.expiresAt()));
    }

    public List<Coupon> list() {
        return couponRepository.findAllByOrderByExpiresAtAsc();
    }

    public Coupon replace(long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon " + id + " not found"));
        coupon.replace(request.code(), request.reward(), request.expiresAt());
        return couponRepository.save(coupon);
    }

    public void delete(long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon " + id + " not found");
        }
        couponRepository.deleteById(id);
    }
}
