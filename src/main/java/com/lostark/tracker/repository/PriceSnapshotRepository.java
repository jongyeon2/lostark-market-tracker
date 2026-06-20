package com.lostark.tracker.repository;

import com.lostark.tracker.domain.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
}
