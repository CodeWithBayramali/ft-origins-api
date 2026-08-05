package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.ConflictException;
import com.codewithbayramali.ftorigins.exception.RegionTakenException;
import com.codewithbayramali.ftorigins.geo.dto.PlaceLabels;
import com.codewithbayramali.ftorigins.geo.dto.RegionResponse;
import com.codewithbayramali.ftorigins.geo.dto.TeamMarkerResponse;
import com.codewithbayramali.ftorigins.geo.model.City;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.repository.RegionRepository;
import com.codewithbayramali.ftorigins.moderation.ProfanityFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RegionService {

    /** Number of kit designs in the client-side KitCatalog; kit ids are 0..KIT_COUNT-1. */
    public static final int KIT_COUNT = 10;

    /** Number of kit patterns in the client-side pattern catalog; pattern ids are 0..PATTERN_COUNT-1. */
    public static final int PATTERN_COUNT = 7;

    /** Team-name bounds (checked AFTER normalization). Short enough to fit a league table, long enough
     *  for "Caferağa Futbol Kulübü". The column is VARCHAR(60); this stays well under it. */
    public static final int TEAM_NAME_MIN = 3;
    public static final int TEAM_NAME_MAX = 30;

    /** Allowed characters: any Unicode letter (Türkçe included) or digit, plus space and a few name
     *  punctuation marks. Excludes emoji and control characters. Spaces are allowed on purpose — a team
     *  name is not a username. */
    private static final Pattern TEAM_NAME_ALLOWED = Pattern.compile("^[\\p{L}\\p{N} .'&-]+$");

    private final RegionRepository regionRepository;
    private final GeoService geoService;
    private final RegionNameGenerator nameGenerator;
    private final ReverseGeocoder reverseGeocoder;
    private final ProfanityFilter profanityFilter;
    private final double joinRadiusKm;
    private final int maxMembers;

    public RegionService(RegionRepository regionRepository,
                         GeoService geoService,
                         RegionNameGenerator nameGenerator,
                         ReverseGeocoder reverseGeocoder,
                         ProfanityFilter profanityFilter,
                         @Value("${app.team.join-radius-km:0.15}") double joinRadiusKm,
                         @Value("${app.team.max-members:8}") int maxMembers) {
        this.regionRepository = regionRepository;
        this.geoService = geoService;
        this.nameGenerator = nameGenerator;
        this.reverseGeocoder = reverseGeocoder;
        this.profanityFilter = profanityFilter;
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
     * <p>
     * A location already covered by a team is never joined silently: unless {@code joinExisting} is
     * set (the player saw the "Join / choose another location" popup and picked Join), this raises
     * {@link RegionTakenException} carrying that team, so the client can ask before the newcomer is
     * added to the team its founder started.
     */
    @Transactional
    public Region getOrCreateForLocation(double lat, double lng, String teamName,
                                         Integer kitHome, Integer kitAway, Integer pattern,
                                         boolean joinExisting) {
        Region joinable = findJoinableTeam(lat, lng);
        if (joinable != null) {
            if (!joinExisting) {
                // Stop and let the client ask. Hand back the existing team so the popup can name it.
                throw new RegionTakenException(RegionResponse.from(joinable));
            }
            return joinable; // player confirmed — join the existing team; its name + kits stay locked, teamName is ignored
        }
        // No team in range → this player FOUNDS one and their name + kit + pattern choices stick.
        return createRegion(geoService.toCell(lat, lng), lat, lng, teamName, kitHome, kitAway, pattern);
    }

    /**
     * Every team whose anchor falls inside a map viewport (bounding box), as lightweight markers for the
     * registration map — each is a founder anchor + the join-radius circle drawn around it. Capped at
     * {@code limit}; when a viewport holds more teams than that, the most-populated are kept (see the
     * repository's ordering). Read-only.
     */
    @Transactional(readOnly = true)
    public List<TeamMarkerResponse> findTeamMarkers(double minLat, double maxLat,
                                                    double minLng, double maxLng, int limit) {
        return regionRepository
                .findMarkersWithinBox(minLat, maxLat, minLng, maxLng, PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(r -> TeamMarkerResponse.from(r, joinRadiusKm))
                .toList();
    }

    /**
     * The team a pin at this location would JOIN (nearest within {@link #joinRadiusKm} with room,
     * else a not-full team in the same H3 cell), or null when the location would FOUND a new team.
     * Read-only preview shared with {@code getOrCreateForLocation} so the pre-registration check and
     * the actual registration agree on who is the founder. Founder = the caller who gets null here.
     */
    @Transactional(readOnly = true)
    public Region findJoinableTeam(double lat, double lng) {
        Region nearby = nearestExistingTeam(lat, lng);
        if (nearby != null) {
            return nearby;
        }
        return regionRepository.findByH3Index(geoService.toCell(lat, lng))
                .filter(r -> r.getPlayerCount() < maxMembers)
                .orElse(null);
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

    private Region createRegion(String cell, double lat, double lng, String teamName, Integer kitHome, Integer kitAway, Integer pattern) {
        boolean founderNamed = teamName != null && !teamName.isBlank();

        Region region = new Region();
        region.setH3Index(cell);
        // Anchor the team at the founder's actual pinned location — what they see under the map pin —
        // NOT the H3 cell centre. The cell (h3Index) is only the uniqueness key; proximity-based
        // membership and the map's team circle both key off this anchor, so it must be the real pin.
        region.setCenterLat(lat);
        region.setCenterLng(lng);
        region.setName(resolveTeamName(teamName, cell));
        region.setNameCustomized(founderNamed);   // a founder-chosen name is not a placeholder to overwrite later
        region.setPlayerCount(0);
        assignKits(region, cell, kitHome, kitAway);
        assignPattern(region, pattern);

        applyPlaceLabels(region, lat, lng);

        try {
            return regionRepository.save(region);
        } catch (DataIntegrityViolationException race) {
            // Two founders raced. If it was the SAME CELL, the other player founded this team first — join
            // theirs. Otherwise it was the team-name unique index: two different cells claimed the same name
            // in the same instant (the existsBy pre-check can't catch a concurrent insert).
            Optional<Region> sameCell = regionRepository.findByH3Index(cell);
            if (sameCell.isPresent()) {
                return sameCell.get();
            }
            throw new ConflictException("TEAM_NAME_TAKEN", "This team name was just taken — please pick another");
        }
    }

    /**
     * The name a new team is created with. A FOUNDER-supplied name is normalized (trimmed, inner
     * whitespace collapsed), validated (length + allowed characters) and checked for uniqueness. A
     * blank name — a joining player, or an old client that predates team naming — falls back to a
     * generated placeholder that is itself made unique.
     * <p>
     * Uniqueness is GLOBAL for now. Once the city layer exists this should narrow to "unique within
     * the city", so a common name like "Yıldızspor" can live once per city instead of once worldwide.
     */
    private String resolveTeamName(String requested, String cell) {
        if (requested == null || requested.isBlank()) {
            return uniqueGeneratedName(cell);
        }
        String name = requested.trim().replaceAll("\\s+", " ");
        if (name.length() < TEAM_NAME_MIN || name.length() > TEAM_NAME_MAX) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TEAM_NAME_LENGTH",
                    "Team name must be " + TEAM_NAME_MIN + "-" + TEAM_NAME_MAX + " characters");
        }
        if (!TEAM_NAME_ALLOWED.matcher(name).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TEAM_NAME_INVALID",
                    "Team name may contain letters, digits, spaces and . ' & - only");
        }
        if (profanityFilter.isProfane(name)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TEAM_NAME_REJECTED",
                    "This team name isn't allowed");
        }
        if (regionRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("TEAM_NAME_TAKEN", "This team name is already taken");
        }
        return name;
    }

    /**
     * A generated placeholder name that does not collide with an existing team. Only a safety net now
     * that founders name their own team; retries a few times, then appends a random suffix so it can
     * never loop forever or fail the unique index.
     */
    private String uniqueGeneratedName(String cell) {
        for (int i = 0; i < 10; i++) {
            String candidate = nameGenerator.generate(cell);
            if (!regionRepository.existsByNameIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return nameGenerator.generate(cell) + " " + UUID.randomUUID().toString().substring(0, 4);
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
     * Locks in the founder's kit pattern. A missing/invalid choice falls back to solid
     * (pattern 0) so legacy teams and old clients render a plain kit rather than a random one.
     */
    private void assignPattern(Region region, Integer pattern) {
        int p = pattern != null && pattern >= 0 && pattern < PATTERN_COUNT ? pattern : 0;
        region.setKitPattern(p);
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
