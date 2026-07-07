package com.lostark.tracker.repository;

import com.lostark.tracker.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Read/write access to admin-registered {@code coupon}s (COUPON-01). The public read surface serves
 * only unexpired coupons soonest-expiry-first; the admin listing keeps expired coupons too.
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Public unexpired coupons, soonest-expiry-first. {@code GreaterThanEqual} keeps the expiry day
     * itself valid — a coupon expiring on {@code today} is still returned through end of that day
     * (D-01, judged on the caller-supplied KST date).
     */
    List<Coupon> findByExpiresAtGreaterThanEqualOrderByExpiresAtAsc(LocalDate today);

    /**
     * All coupons soonest-expiry-first — the admin listing, which includes already-expired coupons.
     */
    List<Coupon> findAllByOrderByExpiresAtAsc();
}
