package com.codewithbayramali.ftorigins.model

import com.codewithbayramali.ftorigins.enums.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "users")
class User : BaseEntity() {

    @Column(nullable = false, unique = true, length = 50)
    var username: String = ""

    @Column(nullable = false, unique = true, length = 100)
    var email: String = ""

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER

    @Column(name = "is_premium", nullable = false)
    var isPremium: Boolean = false

    @Column(name = "premium_expires_at")
    var premiumExpiresAt: Instant? = null

    @Column(nullable = false)
    var coins: Int = 0

    @Column(nullable = false)
    var gems: Int = 0

    @Column(length = 2)
    var country: String? = null

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null
}
