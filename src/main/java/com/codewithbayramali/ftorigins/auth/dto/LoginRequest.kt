package com.codewithbayramali.ftorigins.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val usernameOrEmail: String,

    @field:NotBlank
    @field:Size(max = 128)
    val password: String
)
