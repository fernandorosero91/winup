package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import com.winup.dto.MatchPageDto;
import com.winup.dto.SportSummaryDto;
import com.winup.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchFactory matchFactory;

    @InjectMocks
    private MatchService matchService;

    @Test
    void should_return_page_with_items_total_and_has_more() {
        MatchEntity match = liveMatch();
        Page<MatchEntity> page = new PageImpl<>(List.of(match), PageRequest.of(0, 6), 8);
        when(matchRepository.search(eq("EN VIVO"), eq("Fútbol"), any(Pageable.class))).thenReturn(page);

        MatchPageDto result = matchService.page("EN VIVO", "Fútbol", 0, 6);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().id()).isEqualTo(match.getId());
        assertThat(result.items().getFirst().sport()).isEqualTo("Fútbol");
        assertThat(result.total()).isEqualTo(8);
        assertThat(result.hasMore()).isTrue();
        assertThat(result.page()).isZero();
    }

    @Test
    void should_normalize_upcoming_status_without_accent() {
        when(matchRepository.search(any(), any(), any())).thenReturn(Page.empty());

        matchService.page("PROXIMO", "fútbol", 0, 6);

        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sport = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(matchRepository).search(status.capture(), sport.capture(), pageable.capture());
        assertThat(status.getValue()).isEqualTo("PRÓXIMO");
        assertThat(sport.getValue()).isEqualTo("Fútbol");
        assertThat(pageable.getValue().getPageSize()).isEqualTo(6);
    }

    @Test
    void should_clamp_page_and_size_to_safe_limits() {
        when(matchRepository.search(any(), any(), any())).thenReturn(Page.empty());

        matchService.page("TODOS", "TODOS", -4, 99);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(matchRepository).search(any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void should_return_summary_for_each_supported_sport() {
        when(matchRepository.countBySportAndStatusAndMinuteGreaterThan("Fútbol", "OPEN", 0)).thenReturn(3L);
        when(matchRepository.countBySportAndStatusAndMinute("Fútbol", "OPEN", 0)).thenReturn(2L);
        when(matchRepository.countBySportAndStatus("Fútbol", "CLOSED")).thenReturn(5L);
        when(matchRepository.countBySport("Fútbol")).thenReturn(10L);

        List<SportSummaryDto> result = matchService.sports();

        assertThat(result).hasSize(SportCatalog.SPORTS.size());
        SportSummaryDto football = result.getFirst();
        assertThat(football.name()).isEqualTo("Fútbol");
        assertThat(football.live()).isEqualTo(3);
        assertThat(football.upcoming()).isEqualTo(2);
        assertThat(football.finished()).isEqualTo(5);
        assertThat(football.total()).isEqualTo(10);
    }

    private MatchEntity liveMatch() {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport("Fútbol");
        match.setLeague("LaLiga");
        match.setHomeTeam("Real Madrid");
        match.setAwayTeam("Barcelona");
        match.setHomeShort("RMA");
        match.setAwayShort("BAR");
        match.setHomeScore(1);
        match.setAwayScore(0);
        match.setMinute(32);
        match.setStatus("OPEN");
        match.setStartsAt(Instant.parse("2026-11-11T18:00:00Z"));
        match.setLocalOdds(BigDecimal.valueOf(1.80));
        match.setDrawOdds(BigDecimal.valueOf(3.40));
        match.setAwayOdds(BigDecimal.valueOf(4.10));
        match.setCreatedAt(Instant.parse("2026-11-11T18:00:00Z"));
        match.setUpdatedAt(Instant.parse("2026-11-11T18:00:00Z"));
        return match;
    }
}
