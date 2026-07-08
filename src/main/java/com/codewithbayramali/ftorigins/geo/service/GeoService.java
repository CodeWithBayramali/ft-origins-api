package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.geo.model.City;
import com.codewithbayramali.ftorigins.geo.repository.CityRepository;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spatial helper around the H3 grid:
 *  - maps a coordinate to its cell (= region/team identity),
 *  - exposes a cell's center and neighbouring cells (for match-making fallback),
 *  - reverse-geocodes a coordinate to the nearest known city (label only).
 */
@Service
public class GeoService {

    private final H3Core h3;
    private final int resolution;
    private final CityRepository cityRepository;

    // Cached snapshot of all cities for in-memory nearest-neighbour search.
    private final AtomicReference<List<City>> cityCache = new AtomicReference<>();

    public GeoService(CityRepository cityRepository,
                      @Value("${app.geo.h3-resolution:8}") int resolution) {
        this.cityRepository = cityRepository;
        this.resolution = resolution;
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialise H3 core", e);
        }
    }

    public int resolution() {
        return resolution;
    }

    /** Map a coordinate to its H3 cell address at the configured resolution. */
    public String toCell(double lat, double lng) {
        return h3.latLngToCellAddress(lat, lng, resolution);
    }

    /** Geographic center [lat, lng] of an H3 cell. */
    public double[] cellCenter(String h3Index) {
        LatLng center = h3.cellToLatLng(h3Index);
        return new double[]{center.lat, center.lng};
    }

    /** The cell plus all neighbours out to {@code k} rings — used for match-making fallback. */
    public List<String> neighbours(String h3Index, int k) {
        return h3.gridDisk(h3Index, k);
    }

    /** Nearest loaded city to a coordinate, or {@code null} when no city data is loaded. */
    public City nearestCity(double lat, double lng) {
        List<City> cities = cityCache.get();
        if (cities == null) {
            cities = cityRepository.findAll();
            cityCache.set(cities);
        }
        City best = null;
        double bestDistance = Double.MAX_VALUE;
        for (City city : cities) {
            double distance = haversineKm(lat, lng, city.getLatitude(), city.getLongitude());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = city;
            }
        }
        return best;
    }

    /** Drop the cached city snapshot so the next lookup re-reads from the DB. */
    public void invalidateCityCache() {
        cityCache.set(null);
    }

    /** Great-circle distance in km between two coordinates. */
    public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        return haversineKm(lat1, lng1, lat2, lng2);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
