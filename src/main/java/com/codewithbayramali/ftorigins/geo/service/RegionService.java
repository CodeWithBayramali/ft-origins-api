package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.geo.dto.PlaceLabels;
import com.codewithbayramali.ftorigins.geo.model.City;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegionService {

    /** Number of kit designs in the client-side KitCatalog; kit ids are 0..KIT_COUNT-1. */
    public static final int KIT_COUNT = 10;

    private final RegionRepository regionRepository;
    private final GeoService geoService;
    private final RegionNameGenerator nameGenerator;
    private final ReverseGeocoder reverseGeocoder;
    private final double joinRadiusKm;
    private final int maxMembers;

    public RegionService(RegionRepository regionRepository,
                         GeoService geoService,
                         RegionNameGenerator nameGenerator,
                         ReverseGeocoder reverseGeocoder,
                         @Value("${app.team.join-radius-km:1.0}") double joinRadiusKm,
                         @Value("${app.team.max-members:8}") int maxMembers) {
        this.regionRepository = regionRepository;
        this.geoService = geoService;
        this.nameGenerator = nameGenerator;
        this.reverseGeocoder = reverseGeocoder;
        this.joinRadiusKm = joinRadiusKm;
        this.maxMembers = maxMembers;
    }

    /**
     * Resolve the region (team) for a pinned location.
     * <p>
     * Membership is by proximity, not by grid cell: if any existing team's anchor is within
     * {@code app.team.join-radius-km} of the pin, the player joins the nearest one — so people who
     * register near each other land on the same team even when a grid boundary runs between them.
     * Only when no team is nearby do we found a new one, anchored at this location's H3 cell
     * (the unique h3_index still guards the rare same-cell creation race).
     */
    @Transactional
    public Region getOrCreateForLocation(double lat, double lng, Integer kitHome, Integer kitAway) {
        Region nearby = nearestExistingTeam(lat, lng);
        if (nearby != null) {
            return nearby; // joining an existing team — its kits are already locked
        }
        String cell = geoService.toCell(lat, lng);
        return regionRepository.findByH3Index(cell)
                .filter(r -> r.getPlayerCount() < maxMembers)
                .orElseGet(() -> createRegion(cell, kitHome, kitAway));
    }

    /**
     * Nearest existing team whose anchor is within {@link #joinRadiusKm} of the pin and that
     * still has room ({@code app.team.max-members}), or null. Full teams are skipped so the
     * next-nearest team in range gets the player; if every team in range is full, the caller
     * founds a new one. (A concurrent join can rarely overshoot the cap by one — acceptable.)
     */
    private Region nearestExistingTeam(double lat, double lng) {
        double cosLat = Math.max(0.01, Math.cos(Math.toRadians(lat)));
        double latDelta = joinRadiusKm / 111.0;                 // ~111 km per degree of latitude
        double lngDelta = joinRadiusKm / (111.320 * cosLat);    // longitude degrees shrink toward the poles
        List<Region> candidates = regionRepository.findWithinBox(
                lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta);

        Region nearest = null;
        double best = Double.MAX_VALUE;
        for (Region r : candidates) {
            if (r.getPlayerCount() >= maxMembers) continue; // team is full
            double d = geoService.distanceKm(lat, lng, r.getCenterLat(), r.getCenterLng());
            if (d <= joinRadiusKm && d < best) {
                best = d;
                nearest = r;
            }
        }
        return nearest;
    }

    private Region createRegion(String cell, Integer kitHome, Integer kitAway) {
        double[] center = geoService.cellCenter(cell);

        Region region = new Region();
        region.setH3Index(cell);
        region.setCenterLat(center[0]);
        region.setCenterLng(center[1]);
        region.setName(nameGenerator.generate(cell));
        region.setNameCustomized(false);
        region.setPlayerCount(0);
        assignKits(region, cell, kitHome, kitAway);

        applyPlaceLabels(region, center[0], center[1]);

        try {
            return regionRepository.save(region);
        } catch (DataIntegrityViolationException race) {
            return regionRepository.findByH3Index(cell).orElseThrow(() -> race);
        }
    }

    /**
     * Locks in the founder's two kit designs. Missing/invalid choices fall back to a pair
     * derived from the cell hash so old clients still produce varied, deterministic kits;
     * home and away are always forced apart so the two kits never look identical.
     */
    private void assignKits(Region region, String cell, Integer kitHome, Integer kitAway) {
        int fallback = Math.floorMod(cell.hashCode(), KIT_COUNT);
        int home = kitHome != null && kitHome >= 0 && kitHome < KIT_COUNT ? kitHome : fallback;
        int away = kitAway != null && kitAway >= 0 && kitAway < KIT_COUNT ? kitAway : (home + 5) % KIT_COUNT;
        if (away == home) away = (home + 5) % KIT_COUNT;
        region.setKitHome(home);
        region.setKitAway(away);
    }

    /**
     * Reverse-geocode the cell center into place labels. Tries OSM/Nominatim first (rich
     * neighborhood/district names); on any failure falls back to offline GeoNames nearest-city
     * (city + country only) so a region is never left unlabeled.
     */
    private void applyPlaceLabels(Region region, double lat, double lng) {
        Optional<PlaceLabels> resolved = reverseGeocoder.reverse(lat, lng);
        if (resolved.isPresent()) {
            PlaceLabels labels = resolved.get();
            region.setNeighborhood(labels.getNeighborhood());
            region.setDistrict(labels.getDistrict());
            region.setCityName(labels.getCity());
            region.setCountryCode(labels.getCountryCode());
            return;
        }
        City city = geoService.nearestCity(lat, lng);
        if (city != null) {
            region.setCountryCode(city.getCountryCode());
            region.setCityName(city.getName());
        }
    }
}
