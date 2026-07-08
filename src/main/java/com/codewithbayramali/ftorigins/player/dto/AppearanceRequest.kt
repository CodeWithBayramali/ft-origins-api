package com.codewithbayramali.ftorigins.player.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * Body & appearance selection from the character screen. All fields optional; nulls are left
 * unchanged (on update) or fall back to defaults (on registration). Catalog ids map to Unity assets.
 *
 * Most fields are purely cosmetic, but [heightCm] and [weightKg] also drive a balanced stat
 * trade-off (see BodyModifiers) — they are the only non-cosmetic body inputs.
 */
data class AppearanceRequest(
    @field:Min(0) val skinToneId: Int? = null,
    @field:Min(0) val hairStyleId: Int? = null,
    @field:Min(0) val hairColorId: Int? = null,
    @field:Min(0) val beardId: Int? = null,
    @field:Min(1) @field:Max(99) val kitNumber: Int? = null,
    @field:Min(150) @field:Max(210) val heightCm: Int? = null,
    @field:Min(40) @field:Max(150) val weightKg: Int? = null
)
