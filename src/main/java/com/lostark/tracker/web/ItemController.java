package com.lostark.tracker.web;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.read.LatestPriceService;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.LatestPriceResponse;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Read-only endpoints for tracked items. {@code GET /api/items} lists the active watchlist (API-01);
 * {@code GET /api/items/{id}/latest} serves the newest price through the cache-aside read model
 * (API-02). Inactive items are excluded from the listing — only the active watchlist is part of the
 * public read surface. Item create/delete moved behind {@code /api/admin/items} (D-04); this surface
 * is read-only.
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
