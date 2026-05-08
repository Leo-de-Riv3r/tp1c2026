package com.tacs.tp1c2026.events;

public record CardAvailableEvent(
    String cardId,
    String referenceId,
    String sourceType
) {}
