package com.codewithbayramali.ftorigins.player.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * One attribute a [TrainingDrill] improves, with its base (pre-diminishing) gain. The actual gain a
 * player receives is this value run through the training service's diminishing-returns + cap math.
 */
@Embeddable
class DrillEffect {

    @Enumerated(EnumType.STRING)
    @Column(name = "attribute", nullable = false, length = 16)
    var attribute: TrainableAttribute = TrainableAttribute.PACE

    @Column(name = "base_gain", nullable = false)
    var baseGain: Int = 1
}
