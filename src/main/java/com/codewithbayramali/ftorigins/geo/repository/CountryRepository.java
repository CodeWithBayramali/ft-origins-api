package com.codewithbayramali.ftorigins.geo.repository;

import com.codewithbayramali.ftorigins.geo.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {
}
