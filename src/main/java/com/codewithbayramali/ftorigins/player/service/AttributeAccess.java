package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.TrainableAttribute;

/**
 * Single source of truth for reading/writing a trainable attribute on a {@link Player} by enum — and
 * its decay debt (points lost to inactivity, not yet trained back; see {@link PlayerUpkeepService}).
 */
final class AttributeAccess {

    private AttributeAccess() {
    }

    static int read(Player p, TrainableAttribute attribute) {
        return switch (attribute) {
            case PACE -> p.getPace();
            case SHOOTING -> p.getShooting();
            case PASSING -> p.getPassing();
            case DRIBBLING -> p.getDribbling();
            case DEFENDING -> p.getDefending();
            case PHYSICAL -> p.getPhysical();
            case GOALKEEPING -> p.getGoalkeeping();
            case CURVE -> p.getCurve();
        };
    }

    static void write(Player p, TrainableAttribute attribute, int value) {
        switch (attribute) {
            case PACE -> p.setPace(value);
            case SHOOTING -> p.setShooting(value);
            case PASSING -> p.setPassing(value);
            case DRIBBLING -> p.setDribbling(value);
            case DEFENDING -> p.setDefending(value);
            case PHYSICAL -> p.setPhysical(value);
            case GOALKEEPING -> p.setGoalkeeping(value);
            case CURVE -> p.setCurve(value);
        }
    }

    static int readDebt(Player p, TrainableAttribute attribute) {
        return switch (attribute) {
            case PACE -> p.getDecayDebtPace();
            case SHOOTING -> p.getDecayDebtShooting();
            case PASSING -> p.getDecayDebtPassing();
            case DRIBBLING -> p.getDecayDebtDribbling();
            case DEFENDING -> p.getDecayDebtDefending();
            case PHYSICAL -> p.getDecayDebtPhysical();
            case GOALKEEPING -> p.getDecayDebtGoalkeeping();
            case CURVE -> p.getDecayDebtCurve();
        };
    }

    static void writeDebt(Player p, TrainableAttribute attribute, int value) {
        switch (attribute) {
            case PACE -> p.setDecayDebtPace(value);
            case SHOOTING -> p.setDecayDebtShooting(value);
            case PASSING -> p.setDecayDebtPassing(value);
            case DRIBBLING -> p.setDecayDebtDribbling(value);
            case DEFENDING -> p.setDecayDebtDefending(value);
            case PHYSICAL -> p.setDecayDebtPhysical(value);
            case GOALKEEPING -> p.setDecayDebtGoalkeeping(value);
            case CURVE -> p.setDecayDebtCurve(value);
        }
    }
}
