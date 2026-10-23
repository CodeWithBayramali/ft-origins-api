package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.player.model.Player;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Authorizes founder-only (team manager) actions: scheduling matches, setting lineups, changing the
 * team crest and — in the live phase — substitutions/tactic changes. One place to change if managership
 * ever widens beyond the founder (e.g. appointed captains).
 */
@Component
public class FounderGuard {

    /** True when the player holds the team's founder (manager) seat. */
    public boolean isFounder(Player player, Region region) {
        UUID founderId = region.getFounderPlayerId();
        return founderId != null && founderId.equals(player.getId());
    }

    /** Asserts founder rights; 403 with a stable code otherwise (also when the seat is vacant). */
    public void requireFounder(Player player, Region region) {
        if (!isFounder(player, region)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "NOT_FOUNDER",
                    "Only the team founder can do this");
        }
    }
}
