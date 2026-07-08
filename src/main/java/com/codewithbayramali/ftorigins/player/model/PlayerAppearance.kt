package com.codewithbayramali.ftorigins.player.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * Purely cosmetic appearance — has NO effect on gameplay (fairness rule). Catalog ids; the visual
 * catalog (skin tones, hairstyles, ...) lives in the Unity client. The stat-affecting body axes
 * (height + weight) live on [Player], not here.
 */
@Embeddable
class PlayerAppearance {

    @Column(name = "skin_tone_id", nullable = false)
    var skinToneId: Int = 0

    @Column(name = "hair_style_id", nullable = false)
    var hairStyleId: Int = 0

    @Column(name = "hair_color_id", nullable = false)
    var hairColorId: Int = 0

    /** Catalog id; 0 = none. Maps to the client's beard catalog (CharacterCatalog.Beards). */
    @Column(name = "beard_id", nullable = false)
    var beardId: Int = 0

    @Column(name = "kit_number", nullable = false)
    var kitNumber: Int = 10
}
