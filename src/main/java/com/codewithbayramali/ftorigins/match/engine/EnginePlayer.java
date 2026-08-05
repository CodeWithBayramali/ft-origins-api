package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchRole;

/**
 * One lineup slot as the engine sees it: the kickoff snapshot of EFFECTIVE attributes (body
 * modifiers already applied — see EffectiveAttributes) plus the training energy the player brought
 * into the match. Pure data; the engine never touches entities. AI fillers are ordinary
 * EnginePlayers with standard stats — the engine can't tell the difference, by design.
 */
public record EnginePlayer(int slot, MatchRole role,
                           int pace, int shooting, int passing, int dribbling,
                           int defending, int physical, int goalkeeping, int curve,
                           int energy) {
}
