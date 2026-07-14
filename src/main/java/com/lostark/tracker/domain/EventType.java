package com.lostark.tracker.domain;

/**
 * Kinds of game events that can be correlated with price movements.
 * Persisted as a string (see {@link GameEvent#getEventType()}).
 */
public enum EventType {
    LOA_ON,
    MAJOR_UPDATE,
    SEASON_END,
    BALANCE_PATCH,
    // v1.5(EVT-01) additive — 순수 추가. event_type VARCHAR(40)은 CHECK 제약이 없어 마이그레이션 불필요.
    NEW_CLASS,
    NEW_RAID,
    GENERAL_PATCH
}
