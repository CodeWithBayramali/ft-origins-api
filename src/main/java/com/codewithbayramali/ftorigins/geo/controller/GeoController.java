package com.codewithbayramali.ftorigins.geo.controller;

import com.codewithbayramali.ftorigins.geo.dto.LocationResolveRequest;
import com.codewithbayramali.ftorigins.geo.dto.LocationResolveResponse;
import com.codewithbayramali.ftorigins.geo.dto.RegionResponse;
import com.codewithbayramali.ftorigins.geo.dto.TeamMarkerResponse;
import com.codewithbayramali.ftorigins.geo.service.LocationService;
import com.codewithbayramali.ftorigins.geo.service.RegionService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class GeoController {

    /** Hard cap on markers returned for one viewport — a street-level view never holds more. */
    private static final int MAX_MARKERS = 200;

    private final LocationService locationService;
    private final RegionService regionService;

    public GeoController(LocationService locationService, RegionService regionService) {
        this.locationService = locationService;
        this.regionService = regionService;
    }

    @PostMapping("/location/resolve")
    public ApiResponse<LocationResolveResponse> resolve(@Valid @RequestBody LocationResolveRequest request) {
        return ApiResponse.ok(locationService.resolve(request.getLatitude(), request.getLongitude()));
    }

    /**
     * Team markers inside a map viewport (the registration map draws a team card and its join-radius
     * circle per team).
     * Public — the map is shown before login. Capped at {@link #MAX_MARKERS}; a wider viewport keeps
     * the most-populated teams.
     */
    @GetMapping("/regions/nearby")
    public ApiResponse<List<TeamMarkerResponse>> nearbyTeams(
            @RequestParam double minLat, @RequestParam double maxLat,
            @RequestParam double minLng, @RequestParam double maxLng) {
        return ApiResponse.ok(regionService.findTeamMarkers(minLat, maxLat, minLng, maxLng, MAX_MARKERS));
    }

    @GetMapping("/regions/{id}")
    public ApiResponse<RegionResponse> region(@PathVariable UUID id) {
        return ApiResponse.ok(locationService.getRegion(id));
    }
}
