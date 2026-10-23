package com.codewithbayramali.ftorigins.economy.model

import com.codewithbayramali.ftorigins.model.BaseEntity
import com.codewithbayramali.ftorigins.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * One change to a user's coin balance (the balance itself stays on [User.coins] for cheap reads).
 * (user, [reason], [reference]) is unique, so a grant or a purchase lands at most once.
 */
@Entity
@Table(
    name = "coin_transactions",
    indexes = [Index(name = "idx_coin_transactions_user_created", columnList = "user_id, created_at")],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_coin_transactions_grant", columnNames = ["user_id", "reason", "reference"])
    ]
)
class CoinTransaction : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null

    /** Coins in (positive) or out (negative). */
    @Column(nullable = false)
    var amount: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var reason: CoinReason = CoinReason.DAILY_REWARD

    /** What the change was for: a day, a match id, a boot design id. */
    @Column(nullable = false, length = 64)
    var reference: String = ""
}
