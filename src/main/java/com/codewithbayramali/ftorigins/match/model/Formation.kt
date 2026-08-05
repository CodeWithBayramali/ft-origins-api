package com.codewithbayramali.ftorigins.match.model

import com.codewithbayramali.ftorigins.match.model.MatchRole.DEF
import com.codewithbayramali.ftorigins.match.model.MatchRole.FWD
import com.codewithbayramali.ftorigins.match.model.MatchRole.GK
import com.codewithbayramali.ftorigins.match.model.MatchRole.MID

/**
 * 6v6 formations (1 GK + 5 outfield), named DEF-MID-FWD (the GK is implicit). [roles] maps lineup
 * slot index -> role; slot 0 is always the GK. The engine is squad-size agnostic — it reads
 * [SQUAD_SIZE] and [roles], so a future size change only touches this file (+ a data migration).
 */
enum class Formation(val roles: List<MatchRole>) {
    F_2_2_1(listOf(GK, DEF, DEF, MID, MID, FWD)),
    F_1_2_2(listOf(GK, DEF, MID, MID, FWD, FWD)),
    F_2_1_2(listOf(GK, DEF, DEF, MID, FWD, FWD)),
    F_3_1_1(listOf(GK, DEF, DEF, DEF, MID, FWD));

    companion object {
        const val SQUAD_SIZE = 6
    }
}
