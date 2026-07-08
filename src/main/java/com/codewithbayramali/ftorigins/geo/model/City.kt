package com.codewithbayramali.ftorigins.geo.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Reference data — a populated place from GeoNames (cities1000). Natural key = geonameId.
 * Loaded at startup by [com.codewithbayramali.ftorigins.geo.service.CityDataLoader].
 * Used to reverse-geocode a coordinate into country/city labels.
 */
@Entity
@Table(
    name = "cities",
    indexes = [Index(name = "idx_cities_country", columnList = "country_code")]
)
class City {

    @Id
    @Column(name = "geoname_id", nullable = false)
    var geonameId: Long = 0

    @Column(nullable = false, length = 200)
    var name: String = ""

    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = ""

    @Column(name = "admin1_code", length = 20)
    var admin1Code: String? = null

    @Column(nullable = false)
    var latitude: Double = 0.0

    @Column(nullable = false)
    var longitude: Double = 0.0

    @Column(nullable = false)
    var population: Long = 0
}
