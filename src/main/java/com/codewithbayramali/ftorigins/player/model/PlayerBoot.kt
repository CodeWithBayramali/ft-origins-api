package com.codewithbayramali.ftorigins.player.model

import com.codewithbayramali.ftorigins.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * A boot design owned by a player (bought in the market). The design id indexes the client
 * BootCatalog (0-19). Design 0 is the free default and is never stored — every player owns it
 * implicitly (see BootService).
 */
@Entity
@Table(
    name = "player_boots",
    indexes = [Index(name = "idx_player_boots_player", columnList = "player_id")],
    uniqueConstraints = [UniqueConstraint(name = "uq_player_boots", columnNames = ["player_id", "design_id"])]
)
class PlayerBoot : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    var player: Player? = null

    @Column(name = "design_id", nullable = false)
    var designId: Int = 0
}
