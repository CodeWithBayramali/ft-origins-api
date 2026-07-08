package com.codewithbayramali.ftorigins.geo.dto

/**
 * Reverse-geocoded administrative labels for a coordinate (any field may be null).
 * Ordered finest → coarsest: neighborhood (semt/mahalle) → district (ilçe) → city (il/şehir) → country.
 */
data class PlaceLabels(
    val neighborhood: String?,
    val district: String?,
    val city: String?,
    val countryName: String?,
    val countryCode: String?
) {
    fun hasAnything(): Boolean =
        !neighborhood.isNullOrBlank() || !district.isNullOrBlank() ||
            !city.isNullOrBlank() || !countryCode.isNullOrBlank()
}
