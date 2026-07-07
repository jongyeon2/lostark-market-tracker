package com.lostark.tracker.web.admin;

import com.lostark.tracker.admin.AdminCouponService;
import com.lostark.tracker.web.dto.CouponRequest;
import com.lostark.tracker.web.dto.CouponResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin CRUD for coupons behind the shared-secret gate (COUPON-01): POST 201 + created resource,
 * GET 200 (expires_at asc, includes expired), PUT 200 full replace, DELETE 204 empty body. Binds
 * {@link CouponRequest} (never the entity) to block mass-assignment of id/timestamps. The
 * X-Admin-Secret gate is already applied by SecurityConfig ({@code /api/admin/**} authenticated) —
 * SecurityConfig is unchanged. Never logs request bodies or any header.
 */
@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    public AdminCouponController(AdminCouponService adminCouponService) {
        this.adminCouponService = adminCouponService;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        CouponResponse body = CouponResponse.from(adminCouponService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<CouponResponse> list() {
        return adminCouponService.list().stream().map(CouponResponse::from).toList();
    }

    @PutMapping("/{id}")
    public CouponResponse replace(@PathVariable long id, @Valid @RequestBody CouponRequest request) {
        return CouponResponse.from(adminCouponService.replace(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        adminCouponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
