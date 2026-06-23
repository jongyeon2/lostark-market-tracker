package com.lostark.tracker.web;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.read.LatestPriceService;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.LatestPriceResponse;
import com.lostark.tracker.web.dto.TrackedItemRequest;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Read/write endpoints for tracked items. {@code POST} persists; {@code GET /api/items} lists the
 * active watchlist (API-01); {@code GET /api/items/{id}/latest} serves the newest price through the
 * cache-aside read model (API-02). Inactive items are excluded from the listing — only the active
 * watchlist is part of the public read surface.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final TrackedItemRepository trackedItemRepository;
    private final LatestPriceService latestPriceService;

    public ItemController(TrackedItemRepository trackedItemRepository,
                          LatestPriceService latestPriceService) {
        this.trackedItemRepository = trackedItemRepository;
        this.latestPriceService = latestPriceService;
    }

    @PostMapping
    public ResponseEntity<TrackedItemResponse> create(@Valid @RequestBody TrackedItemRequest request) {
        TrackedItem saved = trackedItemRepository.save(
                new TrackedItem(request.externalItemId(), request.displayName(), request.category()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TrackedItemResponse.from(saved));
    }

    @GetMapping
    public List<TrackedItemResponse> list() {
        return trackedItemRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(TrackedItem::getDisplayName))
                .map(TrackedItemResponse::from)
                .toList();
    }

    @GetMapping("/{id}/latest")
    public LatestPriceResponse latest(@PathVariable long id) {
        return latestPriceService.latest(id);
    }
}
