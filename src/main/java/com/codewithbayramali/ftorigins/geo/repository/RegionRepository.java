package com.codewithbayramali.ftorigins.geo.repository;

import com.codewithbayramali.ftorigins.geo.model.Region;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {

    Optional<Region> findByH3Index(String h3Index);

    /** Case-insensitive team-name existence check (backed by the uq_regions_name_ci functional index). */
    boolean existsByNameIgnoreCase(String name);

    /** Regions whose anchor falls inside a lat/lng bounding box — pre-filter for radius search. */
    @Query("select r from Region r where r.centerLat between :minLat and :maxLat "
            + "and r.centerLng between :minLng and :maxLng")
    List<Region> findWithinBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                               @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    /**
     * Team markers inside a map viewport, capped via {@code Pageable}. Ordered by player count desc so
     * that when a wide viewport holds more teams than the cap, the ones kept are the most-populated
     * (most relevant) rather than an arbitrary slice.
     */
    @Query("select r from Region r where r.centerLat between :minLat and :maxLat "
            + "and r.centerLng between :minLng and :maxLng order by r.playerCount desc, r.id")
    List<Region> findMarkersWithinBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                      @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                      Pageable pageable);
}
