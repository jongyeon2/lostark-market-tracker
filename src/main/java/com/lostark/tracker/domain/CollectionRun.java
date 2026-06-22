package com.lostark.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Run history: one row per collection tick recording start/finish and per-item outcome
 * counts (DATA-04). Populated by the scheduler in Phase 2; the model is locked here.
 */
@Entity
@Table(name = "collection_run")
public class CollectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "items_attempted", nullable = false)
    private int itemsAttempted;

    @Column(name = "items_succeeded", nullable = false)
    private int itemsSucceeded;

    @Column(name = "items_failed", nullable = false)
    private int itemsFailed;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    protected CollectionRun() {
    }

    public CollectionRun(OffsetDateTime startedAt, OffsetDateTime finishedAt,
                         int itemsAttempted, int itemsSucceeded, int itemsFailed, String status) {
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.itemsAttempted = itemsAttempted;
        this.itemsSucceeded = itemsSucceeded;
        this.itemsFailed = itemsFailed;
        this.status = status;
    }

    /**
     * Terminal update of the run lifecycle (D-12): set finish time, per-item counts, and the
     * count-based status. {@code itemsAttempted} is fixed at creation; this records the outcome.
     */
    public void finish(OffsetDateTime finishedAt, int itemsSucceeded, int itemsFailed, String status) {
        this.finishedAt = finishedAt;
        this.itemsSucceeded = itemsSucceeded;
        this.itemsFailed = itemsFailed;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public int getItemsAttempted() {
        return itemsAttempted;
    }

    public int getItemsSucceeded() {
        return itemsSucceeded;
    }

    public int getItemsFailed() {
        return itemsFailed;
    }

    public String getStatus() {
        return status;
    }
}
