package com.lostark.tracker.domain;

/**
 * Kinds of game events that can be correlated with price movements.
 * Persisted as a string (see {@link GameEvent#getEventType()}).
 */
public enum EventType {
    LOA_ON,
    MAJOR_UPDATE,
    SEASON_END,
    BALANCE_PATCH
}
