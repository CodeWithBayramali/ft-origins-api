package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.geo.dto.CrestDto;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.repository.CityRepository;
import com.codewithbayramali.ftorigins.geo.repository.RegionRepository;
import com.codewithbayramali.ftorigins.moderation.ProfanityFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegionServiceTest {

    private static final double LAT = 36.91975;
    private static final double LNG = 30.66913;
    private static final double JOIN_RADIUS_KM = 0.075;
    private static final CrestDto CREST = new CrestDto(0, 12, 3, 0);

    private RegionRepository regions;
    private GeoService geo;
    private RegionService service;

    @BeforeEach
    void setUp() {
        regions = mock(RegionRepository.class);
        geo = new GeoService(mock(CityRepository.class), 11);
        ReverseGeocoder reverse = mock(ReverseGeocoder.class);
        when(reverse.reverse(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        service = new RegionService(regions, geo, mock(RegionNameGenerator.class), reverse,
                mock(ProfanityFilter.class), new TeamCrestPolicy(), JOIN_RADIUS_KM, 10);

        when(regions.findByH3Index(anyString())).thenReturn(Optional.empty());
        when(regions.findWithinBox(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of());
        when(regions.save(any(Region.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aSpotWhereAFullTeamHoldsTheCellStartsANewTeamUnderAFreeCellNextToIt() {
        String cell = geo.toCell(LAT, LNG);
        Region full = team(cell, LAT, LNG, 10);
        when(regions.findByH3Index(cell)).thenReturn(Optional.of(full));
        inRange(full);

        Region created = service.getOrCreateForLocation(LAT, LNG, "Yeni Takım", 1, 2, 0, null, false);

        assertThat(created).isNotSameAs(full);
        assertThat(created.getH3Index()).isNotEqualTo(cell).isIn(geo.neighbours(cell, 1));
        assertThat(created.getCenterLat()).isEqualTo(LAT);    // still anchored at the founder's pin
        assertThat(created.getCenterLng()).isEqualTo(LNG);
        assertThat(created.getName()).isEqualTo("Yeni Takım");
    }

    @Test
    void sharingATeamsCellOutsideItsCircleDoesNotJoinIt() {
        // The team's anchor is ~150 m north: outside its 75 m circle, though the repository says the
        // pin's cell is the team's (the old same-cell rule would have joined it).
        Region nearby = team("8b2d1a2b3c4dfff", LAT + 0.00135, LNG, 4);
        when(regions.findByH3Index(geo.toCell(LAT, LNG))).thenReturn(Optional.of(nearby));
        inRange(nearby);

        assertThat(service.findJoinableTeam(LAT, LNG)).isNull();
        Region created = service.getOrCreateForLocation(LAT, LNG, "Yeni Takım", 1, 2, 0, null, false);
        assertThat(created).isNotSameAs(nearby);
        assertThat(created.getCenterLat()).isEqualTo(LAT);
    }

    @Test
    void aSpotInAnEmptyCellIsFiledUnderItsOwnCell() {
        Region created = service.getOrCreateForLocation(LAT, LNG, "Yeni Takım", 1, 2, 0, null, false);

        assertThat(created.getH3Index()).isEqualTo(geo.toCell(LAT, LNG));
    }

    @Test
    void aTeamWithRoomInsideItsCircleIsJoinedNotFounded() {
        Region open = team(geo.toCell(LAT, LNG), LAT + 0.0005, LNG, 4);   // ~55 m away
        inRange(open);

        assertThat(service.getOrCreateForLocation(LAT, LNG, null, null, null, null, null, true)).isSameAs(open);
    }

    @Test
    void aFoundersCrestIsStoredOnTheNewTeam() {
        Region created = service.getOrCreateForLocation(LAT, LNG, "Yeni Takım", 1, 2, 0, CREST, false);

        assertThat(created.getCrestShape()).isEqualTo(0);
        assertThat(created.getCrestSymbol()).isEqualTo(12);
        assertThat(created.getCrestBase()).isEqualTo(3);
        assertThat(created.getCrestDetail()).isEqualTo(0);
        assertThat(CrestDto.from(created)).isEqualTo(CREST);
    }

    @Test
    void aFounderWhoSendsNoCrestFoundsATeamWithoutOne() {
        Region created = service.getOrCreateForLocation(LAT, LNG, "Yeni Takım", 1, 2, 0, null, false);

        assertThat(CrestDto.from(created)).isNull();   // no fallback crest, unlike kits
    }

    @Test
    void aJoiningPlayersCrestIsIgnored() {
        Region open = team(geo.toCell(LAT, LNG), LAT + 0.0005, LNG, 4);   // ~55 m away
        new TeamCrestPolicy().apply(open, new CrestDto(5, 40, 1, 2));     // the founder's crest
        inRange(open);

        Region joined = service.getOrCreateForLocation(LAT, LNG, null, null, null, null, CREST, true);

        assertThat(joined).isSameAs(open);
        assertThat(CrestDto.from(joined)).isEqualTo(new CrestDto(5, 40, 1, 2));
        verify(regions, never()).save(any(Region.class));
    }

    @Test
    void aCrestWhoseSymbolColourMatchesItsBaseIsRejected() {
        CrestDto invisibleSymbol = new CrestDto(0, 12, 3, 3);

        assertThatThrownBy(() -> service.getOrCreateForLocation(LAT, LNG, "Yeni Takım", 1, 2, 0, invisibleSymbol, false))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("CREST_COLOURS_SAME");
                });
        verify(regions, never()).save(any(Region.class));
    }

    @Test
    void aCityWideBoxGetsNoMarkersAndNeverReachesTheDatabase() {
        assertThat(service.findTeamMarkers(40.8, 41.4, 28.6, 29.3, 200)).isEmpty();
        verify(regions, never()).findMarkersWithinBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
    }

    private void inRange(Region team) {
        when(regions.findWithinBox(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(team));
    }

    private static Region team(String cell, double lat, double lng, int players) {
        Region r = new Region();
        r.setId(UUID.randomUUID());
        r.setH3Index(cell);
        r.setName("Kırmızı Şeytanlar");
        r.setCenterLat(lat);
        r.setCenterLng(lng);
        r.setPlayerCount(players);
        return r;
    }
}
