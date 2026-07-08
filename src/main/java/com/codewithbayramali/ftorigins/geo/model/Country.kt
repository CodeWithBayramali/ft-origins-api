package com.codewithbayramali.ftorigins.geo.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Reference data — ISO 3166-1 country. Natural key = alpha-2 code.
 * Seeded via Flyway (V3). Used only as a label/grouping for regions.
 */
@Entity
@Table(name = "countries")
class Country {

    @Id
    @Column(length = 2, nullable = false)
    var code: String = ""

    @Column(nullable = false, length = 100)
    var name: String = ""
}
