package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.match.model.Formation;
import com.codewithbayramali.ftorigins.match.model.MatchRole;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.service.EffectiveAttributes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the best lineup the system can pick for a team when no founder did: the GET-lineup
 * suggestion, and (in the kickoff phase) the fallback for sides whose founder never submitted.
 * Slots that no member can fill are left to standard-stat AI fillers ({@code player == null}) —
 * teams never play short-handed.
 */
@Component
public class DefaultLineupFactory {

    public static final Formation DEFAULT_FORMATION = Formation.F_2_2_1;

    /** One picked slot; {@code player} null = AI filler. */
    public record Slot(int slot, MatchRole role, Player player) {
    }

    /**
     * Greedy pick, slot order (GK first): each slot takes the unassigned member with the best
     * role-weighted score. Good enough on a max-8-member pool — an optimal assignment would move
     * the needle less than the stats themselves do.
     */
    public List<Slot> build(Formation formation, List<Player> members) {
        List<Slot> slots = new ArrayList<>(formation.getRoles().size());
        Set<Player> assigned = new HashSet<>();
        for (int i = 0; i < formation.getRoles().size(); i++) {
            MatchRole role = formation.getRoles().get(i);
            Player best = null;
            int bestScore = -1;
            for (Player member : members) {
                if (assigned.contains(member)) {
                    continue;
                }
                int score = roleScore(role, EffectiveAttributes.of(member));
                if (score > bestScore) {
                    best = member;
                    bestScore = score;
                }
            }
            if (best != null) {
                assigned.add(best);
            }
            slots.add(new Slot(i, role, best));
        }
        return slots;
    }

    /** How well a set of effective attributes fits a role — the weights are the role's job description. */
    public static int roleScore(MatchRole role, EffectiveAttributes a) {
        return switch (role) {
            case GK -> a.goalkeeping();
            case DEF -> Math.round(0.50f * a.defending() + 0.30f * a.physical() + 0.20f * a.pace());
            case MID -> Math.round(0.35f * a.passing() + 0.25f * a.dribbling() + 0.20f * a.defending() + 0.20f * a.pace());
            case FWD -> Math.round(0.40f * a.shooting() + 0.25f * a.dribbling() + 0.25f * a.pace() + 0.10f * a.passing());
        };
    }
}
