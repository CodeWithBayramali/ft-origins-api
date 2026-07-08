package com.codewithbayramali.ftorigins.auth.dto

import com.codewithbayramali.ftorigins.enums.UserRole
import com.codewithbayramali.ftorigins.model.User
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val role: UserRole,
    val isPremium: Boolean,
    val premiumExpiresAt: Instant?,
    val coins: Int,
    val gems: Int,
    val createdAt: Instant?,
    val birthDate: LocalDate?,
    val age: Int?
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserResponse = UserResponse(
            id = requireNotNull(user.id),
            username = user.username,
            email = user.email,
            role = user.role,
            isPremium = user.isPremium,
            premiumExpiresAt = user.premiumExpiresAt,
            coins = user.coins,
            gems = user.gems,
            createdAt = user.createdAt,
            birthDate = user.birthDate,
            age = user.birthDate?.let { Period.between(it, LocalDate.now()).years }
        )
    }
}
