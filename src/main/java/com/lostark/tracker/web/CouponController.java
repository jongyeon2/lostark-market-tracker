package com.lostark.tracker.web;

import com.lostark.tracker.repository.CouponRepository;
import com.lostark.tracker.web.dto.CouponResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Public read endpoint for coupons (COUPON-02/03). {@code GET /api/coupons} returns only unexpired
 * coupons, soonest-expiry-first, and excludes expired ones. Injects {@link CouponRepository} directly
 * (the {@link ItemController} precedent for read controllers). Validity is judged on the server's KST
 * date so a coupon expiring today stays visible through end of that day (D-01). permitAll via
 * SecurityConfig {@code anyRequest().permitAll()} — SecurityConfig is unchanged. The display cap (≤6)
 * is the dashboard's responsibility (17.3-03); this endpoint returns all valid coupons sorted.
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CouponRepository couponRepository;

    public CouponController(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @GetMapping
    public List<CouponResponse> list() {
        return couponRepository
                .findByExpiresAtGreaterThanEqualOrderByExpiresAtAsc(LocalDate.now(KST))
                .stream()
                .map(CouponResponse::from)
                .toList();
    }
}
