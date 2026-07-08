package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.dto.LocationResolveResponse;
import com.codewithbayramali.ftorigins.geo.dto.RegionResponse;
import com.codewithbayramali.ftorigins.geo.model.City;
import com.codewithbayramali.ftorigins.geo.model.Country;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.repository.CountryRepository;
import com.codewithbayramali.ftorigins.geo.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LocationService {

    private final GeoService geoService;
    private final RegionRepository regionRepository;
    private final CountryRepository countryRepository;

    public LocationService(GeoService geoService,
                           RegionRepository regionRepository,
                           CountryRepository countryRepository) {
        this.geoService = geoService;
        this.regionRepository = regionRepository;
        this.countryRepository = countryRepository;
    }

    /** Preview the cell + labels for a coordinate WITHOUT creating a region (used before registration). */
    @Transactional(readOnly = true)
    public LocationResolveResponse resolve(double latitude, double longitude) {
        String cell = geoService.toCell(latitude, longitude);
        double[] center = geoService.cellCenter(cell);

        City city = geoService.nearestCity(center[0], center[1]);
        String countryCode = city != null ? city.getCountryCode() : null;
        String cityName = city != null ? city.getName() : null;
        String countryName = countryCode != null
                ? countryRepository.findById(countryCode).map(Country::getName).orElse(null)
                : null;

        RegionResponse region = regionRepository.findByH3Index(cell)
                .map(RegionResponse::from)
                .orElse(null);

        return new LocationResolveResponse(cell, center[0], center[1], countryCode, countryName, cityName, region);
    }

    @Transactional(readOnly = true)
    public RegionResponse getRegion(UUID id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("REGION_NOT_FOUND", "Region not found"));
        return RegionResponse.from(region);
    }
}
