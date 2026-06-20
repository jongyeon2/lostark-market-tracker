package com.lostark.tracker.web;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.TrackedItemRequest;
import com.lostark.tracker.web.dto.TrackedItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Thin walking-skeleton round-trip for tracked items: POST persists, GET lists — both through
 * the live DB via Spring Data JPA. The full read API (caching, timeline) is Phase 3.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final TrackedItemRepository trackedItemRepository;

    public ItemController(TrackedItemRepository trackedItemRepository) {
        this.trackedItemRepository = trackedItemRepository;
    }

    @PostMapping
    public ResponseEntity<TrackedItemResponse> create(@Valid @RequestBody TrackedItemRequest request) {
        TrackedItem saved = trackedItemRepository.save(
                new TrackedItem(request.externalItemId(), request.displayName(), request.category()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TrackedItemResponse.from(saved));
    }

    @GetMapping
    public List<TrackedItemResponse> list() {
        return trackedItemRepository.findAll().stream()
                .map(TrackedItemResponse::from)
                .toList();
    }
}
