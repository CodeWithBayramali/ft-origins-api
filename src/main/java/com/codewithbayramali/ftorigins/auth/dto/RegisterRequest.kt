package com.codewithbayramali.ftorigins.auth.dto

import com.codewithbayramali.ftorigins.player.dto.AppearanceRequest
import com.codewithbayramali.ftorigins.player.model.Foot
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class RegisterRequest(
    // Doubles as the in-match player name shown above the character, so it is kept short.
    @field:NotBlank
    @field:Size(min = 3, max = 16)
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_.-]+$",
        message = "username may contain letters, digits, dot, dash, underscore only"
    )
    val username: String,

    @field:NotBlank
    @field:Email
    @field:Size(max = 100)
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 128, message = "password must be 8-128 characters")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "password must contain at least one letter and one digit"
    )
    val password: String,

    @field:NotNull
    @field:Past
    val birthDate: LocalDate?,

    // Home location pinned on the map at registration — assigns the player's region (team).
    @field:NotNull
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double?,

    @field:NotNull
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double?,

    // Preferred (dominant) foot chosen on the character screen; defaults to RIGHT when omitted.
    val preferredFoot: Foot? = null,

    @field:Size(max = 60)
    val displayName: String? = null,

    // Team NAME — the founder names the team, same as they pick its kit. Only honoured when this
    // registration FOUNDS a new team; ignored when joining an existing one (its name is already set).
    // Spaces are allowed ("Caferağa FK") — it is not a username. Must be unique. The upper bound here
    // is a loose DoS guard; the real length/character/uniqueness rules run in RegionService after the
    // name is normalized (trimmed, inner whitespace collapsed). Optional at the API level so a JOINING
    // player omits it and an older client still registers; a founder who omits it falls back to a
    // generated name.
    @field:Size(max = 40)
    val teamName: String? = null,

    // Team kit choices (KitCatalog ids). Only honoured when this registration FOUNDS a new
    // team; ignored when the player joins an existing one (its kits are already locked).
    @field:Min(0) @field:Max(9)
    val kitHomeId: Int? = null,

    @field:Min(0) @field:Max(9)
    val kitAwayId: Int? = null,

    // Kit pattern (fabric design) chosen on the character screen. Only honoured when this
    // registration FOUNDS a new team; ignored when joining an existing one.
    @field:Min(0) @field:Max(6)
    val patternId: Int? = null,

    // Consent to JOIN the team that already covers this location instead of founding a new one.
    // Registration never joins an existing team silently: when the pinned location falls inside a
    // team's join radius and consent is absent (null/omitted/false — the client pinned what it
    // believed was an empty spot and was founding), the API stops with REGION_TAKEN and hands that
    // team back so the client can show a "Join / Choose another location" popup. The client re-submits
    // with joinExisting = true once the player picks "Join". Ignored when the location founds a new
    // team anyway (no team in range). Nullable (like the other optional fields) so a client that
    // serializes it as null still registers — null is treated as "no consent", i.e. false.
    val joinExisting: Boolean? = false,

    @field:Valid
    val appearance: AppearanceRequest? = null
)
