package com.lostark.tracker.web.admin;

import com.lostark.tracker.admin.AdminItemService;
import com.lostark.tracker.admin.AdminItemService.UpsertResult;
import com.lostark.tracker.web.dto.TrackedItemRequest;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin write surface for watchlist items behind the (04-02) shared-secret gate (ADMIN-02, D-04):
 * POST creates (201) or reactivates a soft-deleted item (200) — 409 on an active duplicate;
 * DELETE soft-deletes (204). Reuses {@link TrackedItemRequest} (never the entity) so id/active are
 * never mass-assigned. The public POST /api/items is removed; the public surface is read-only GET.
 */
@RestController
@RequestMapping("/api/admin/items")
public class AdminItemController {

    private final AdminItemService adminItemService;

    public AdminItemController(AdminItemService adminItemService) {
        this.adminItemService = adminItemService;
    }

    @GetMapping
    public List<TrackedItemResponse> list() {
        return adminItemService.listAll().stream().map(TrackedItemResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<TrackedItemResponse> create(@Valid @RequestBody TrackedItemRequest request) {
        UpsertResult result = adminItemService.create(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TrackedItemResponse.from(result.item()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        adminItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}