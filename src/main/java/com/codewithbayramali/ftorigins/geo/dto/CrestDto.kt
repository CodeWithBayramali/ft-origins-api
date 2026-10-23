package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.geo.model.Region
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * A team crest ("arma"): four ids into the client-side crest catalogs — the [shape], the [symbol] drawn
 * on it, the [base] colour and the [detail] colour the symbol is drawn in. One shape both ways: the
 * request body (registration's `crest`, PUT /teams/mine/crest) and the `crest` / `teamCrest` field of
 * team responses, which is null when the team has none. Base and detail must also differ, or the symbol
 * vanishes into its background — annotations can't say that, so TeamCrestPolicy checks it.
 */
data class CrestDto(
    @field:NotNull @field:Min(0) @field:Max(SHAPE_COUNT - 1L)
    val shape: Int?,

    @field:NotNull @field:Min(0) @field:Max(SYMBOL_COUNT - 1L)
    val symbol: Int?,

    @field:NotNull @field:Min(0) @field:Max(COLOUR_COUNT - 1L)
    val base: Int?,

    @field:NotNull @field:Min(0) @field:Max(COLOUR_COUNT - 1L)
    val detail: Int?
) {
    companion object {
        /** Catalog sizes, in lockstep with the client crest editor (ids are 0..COUNT-1) — the only
         *  place these limits live. Growing a catalog = raise its count here and ship the client art. */
        const val SHAPE_COUNT = 12
        const val SYMBOL_COUNT = 64
        const val COLOUR_COUNT = 16

        /** The team's crest, or null when it has none (its crest columns are null). */
        @JvmStatic
        fun from(region: Region): CrestDto? {
            val shape = region.crestShape ?: return null
            val symbol = region.crestSymbol ?: return null
            val base = region.crestBase ?: return null
            val detail = region.crestDetail ?: return null
            return CrestDto(shape, symbol, base, detail)
        }
    }
}
