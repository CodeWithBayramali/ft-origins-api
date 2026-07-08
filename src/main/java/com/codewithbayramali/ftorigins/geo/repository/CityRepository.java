package com.codewithbayramali.ftorigins.geo.repository;

import com.codewithbayramali.ftorigins.geo.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
}
