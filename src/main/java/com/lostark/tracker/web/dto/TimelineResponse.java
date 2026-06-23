package com.lostark.tracker.web.dto;

import java.util.List;

/**
 * The timeline read payload: TWO independent arrays (D-04). {@code snapshots} is the window's price
 * line (ascending by {@code collectedAt}); {@code events} is the game events overlapping the window.
 * Kept distinct so a client overlays event markers on the price chart, and so 03-03 downsampling can
 * shrink {@code snapshots} without touching {@code events}.
 */
public record TimelineResponse(
        List<SnapshotPoint> snapshots,
        List<EventPoint> events
) {
}
