package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchEventType;

import java.util.Map;

/**
 * One timeline entry as produced by the engine. The payload is an ordered map (insertion order is
 * deterministic) that the persistence layer serializes to the match_events JSONB column; actors are
 * referenced as side + slot, which the client resolves against the lineup. The engine does not
 * assign seq — that is the persistence layer's global counter.
 */
public record EngineEvent(int minute, int second, MatchEventType type, Map<String, Object> payload) {
}
