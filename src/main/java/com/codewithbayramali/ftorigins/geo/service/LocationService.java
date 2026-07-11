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
    private final RegionService regionService;

    public LocationService(GeoService geoService,
                           RegionRepository regionRepository,
                           CountryRepository countryRepository,
                           RegionService regionService) {
        this.geoService = geoService;
        this.regionRepository = regionRepository;
        this.countryRepository = countryRepository;
        this.regionService = regionService;
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

        // The team this pin would JOIN (proximity-based, same rule as registration), or null when
        // the pin would FOUND a new team. Null here == the caller is the founder and may pick the kit.
        Region joinable = regionService.findJoinableTeam(latitude, longitude);
        RegionResponse region = joinable != null ? RegionResponse.from(joinable) : null;

        return new LocationResolveResponse(cell, center[0], center[1], countryCode, countryName, cityName,
                region, joinable == null);
    }

    @Transactional(readOnly = true)
    public RegionResponse getRegion(UUID id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("REGION_NOT_FOUND", "Region not found"));
        return RegionResponse.from(region);
    }
}
