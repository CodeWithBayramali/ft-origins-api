package com.codewithbayramali.ftorigins.auth.dto

import java.time.Instant

data class AuthResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val user: UserResponse
)
