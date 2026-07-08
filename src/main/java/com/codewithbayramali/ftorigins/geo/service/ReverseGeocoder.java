package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.geo.dto.PlaceLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Reverse-geocodes a coordinate into administrative labels (neighborhood / district / city / country)
 * via an OSM Nominatim endpoint. Used at region creation to give each team a recognizable place name.
 *
 * Called once per brand-new H3 cell (registration only), so volume is low — well within Nominatim's
 * usage policy. Always best-effort: any failure / timeout / disabled flag returns empty, and the caller
 * falls back to offline GeoNames data so registration never breaks. Swap {@code app.geo.nominatim.url}
 * to a self-hosted / paid instance for production scale.
 */
@Service
public class ReverseGeocoder {

    private static final Logger log = LoggerFactory.getLogger(ReverseGeocoder.class);

    // zoom 16 ≈ neighbourhood/suburb detail — the sweet spot for semt/mahalle labels.
    private static final int ZOOM = 16;

    private final boolean enabled;
    private final String baseUrl;
    private final String userAgent;
    private final String language;
    private final ObjectMapper objectMapper;
    private final HttpClient http;

    public ReverseGeocoder(
            @Value("${app.geo.nominatim.enabled:true}") boolean enabled,
            @Value("${app.geo.nominatim.url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.geo.nominatim.user-agent:ft-origins/1.0}") String userAgent,
            @Value("${app.geo.nominatim.language:en}") String language,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.userAgent = userAgent;
        this.language = language;
        this.objectMapper = objectMapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    /** Reverse-geocode a coordinate. Empty when disabled, on any error/timeout, or when no labels resolve. */
    @SuppressWarnings("unchecked")
    public Optional<PlaceLabels> reverse(double lat, double lng) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            String url = baseUrl + "/reverse?format=jsonv2&zoom=" + ZOOM + "&addressdetails=1"
                    + "&lat=" + lat + "&lon=" + lng
                    + "&accept-language=" + URLEncoder.encode(language, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", userAgent)   // required by Nominatim's usage policy
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                log.warn("Nominatim reverse {},{} -> HTTP {}", lat, lng, resp.statusCode());
                return Optional.empty();
            }

            Map<String, Object> root = objectMapper.readValue(resp.body(), Map.class);
            Object addr = root.get("address");
            if (!(addr instanceof Map)) {
                return Optional.empty();
            }
            Map<String, Object> a = (Map<String, Object>) addr;

            String neighborhood = firstOf(a, "neighbourhood", "suburb", "quarter", "residential", "city_block");
            String district = firstOf(a, "city_district", "town", "municipality", "district", "borough", "county");
            String city = firstOf(a, "city", "province", "state", "region");
            String countryName = firstOf(a, "country");
            String cc = firstOf(a, "country_code");

            PlaceLabels labels = new PlaceLabels(
                    neighborhood, district, city, countryName,
                    cc != null ? cc.toUpperCase() : null);
            return labels.hasAnything() ? Optional.of(labels) : Optional.empty();
        } catch (Exception e) {
            log.warn("Nominatim reverse failed for {},{}: {}", lat, lng, e.toString());
            return Optional.empty();
        }
    }

    /** First non-blank value among the given address keys, trimmed; null if none present. */
    private static String firstOf(Map<String, Object> address, String... keys) {
        for (String key : keys) {
            Object v = address.get(key);
            if (v != null) {
                String s = v.toString().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }
}
