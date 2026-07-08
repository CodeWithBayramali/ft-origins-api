package com.codewithbayramali.ftorigins.geo.controller;

import com.codewithbayramali.ftorigins.geo.dto.LocationResolveRequest;
import com.codewithbayramali.ftorigins.geo.dto.LocationResolveResponse;
import com.codewithbayramali.ftorigins.geo.dto.RegionResponse;
import com.codewithbayramali.ftorigins.geo.service.LocationService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class GeoController {

    private final LocationService locationService;

    public GeoController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/location/resolve")
    public ApiResponse<LocationResolveResponse> resolve(@Valid @RequestBody LocationResolveRequest request) {
        return ApiResponse.ok(locationService.resolve(request.getLatitude(), request.getLongitude()));
    }

    @GetMapping("/regions/{id}")
    public ApiResponse<RegionResponse> region(@PathVariable UUID id) {
        return ApiResponse.ok(locationService.getRegion(id));
    }
}
