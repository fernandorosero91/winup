package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.exception.ValidationException;
import com.winup.dto.CreateMatchRequest;
import com.winup.dto.MatchDto;
import com.winup.dto.OddsDto;
import com.winup.dto.UpdateOddsRequest;
import com.winup.mapper.MatchMapper;
import com.winup.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMatchServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T22:00:00Z");

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private MatchOddsSnapshotService snapshotService;

    @Test
    void should_create_a_live_match_with_the_requested_odds() {
        when(matchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchMapper.toDto(any())).thenReturn(dto());
        CreateMatchRequest request = new CreateMatchRequest(
                "Fútbol", "LaLiga", "Real Madrid", "Barcelona",
                new BigDecimal("1.80"), new BigDecimal("3.50"), new BigDecimal("4.20"), 1
        );

        service().create(request);

        org.mockito.ArgumentCaptor<MatchEntity> captor = org.mockito.ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(captor.capture());
        MatchEntity saved = captor.getValue();
        assertThat(saved.getSport()).isEqualTo("Fútbol");
        assertThat(saved.getHomeTeam()).isEqualTo("Real Madrid");
        assertThat(saved.getLocalOdds()).isEqualByComparingTo("1.80");
        assertThat(saved.getDrawOdds()).isEqualByComparingTo("3.50");
        assertThat(saved.getAwayOdds()).isEqualByComparingTo("4.20");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(saved.getSource()).isEqualTo("ADMIN");
        verify(eventPublisher).matchCreated(saved);
    }

    @Test
    void should_reject_a_match_without_a_valid_sport() {
        CreateMatchRequest request = new CreateMatchRequest(
                "Curling", "Liga", "A", "B",
                new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("2.50"), 1
        );

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(ResponseStatusException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void should_reject_odds_below_one() {
        CreateMatchRequest request = new CreateMatchRequest(
                "Fútbol", "LaLiga", "A", "B",
                new BigDecimal("0.90"), new BigDecimal("3.00"), new BigDecimal("2.50"), 1
        );

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(ValidationException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void should_reject_the_same_team_on_both_sides() {
        CreateMatchRequest request = new CreateMatchRequest(
                "Fútbol", "LaLiga", "Real Madrid", "real madrid",
                new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("2.50"), 1
        );

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void should_update_the_odds_of_an_existing_match() {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport("Fútbol");
        match.setLocalOdds(new BigDecimal("2.00"));
        match.setDrawOdds(new BigDecimal("3.00"));
        match.setAwayOdds(new BigDecimal("4.00"));
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchRepository.save(match)).thenReturn(match);
        when(matchMapper.toDto(match)).thenReturn(dto());

        service().updateOdds(match.getId(), new UpdateOddsRequest(
                new BigDecimal("1.50"), new BigDecimal("4.10"), new BigDecimal("6.00")
        ));

        assertThat(match.getLocalOdds()).isEqualByComparingTo("1.50");
        assertThat(match.getDrawOdds()).isEqualByComparingTo("4.10");
        assertThat(match.getAwayOdds()).isEqualByComparingTo("6.00");
    }

    private AdminMatchService service() {
        return new AdminMatchService(
                matchRepository,
                eventPublisher,
                matchMapper,
                snapshotService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private MatchDto dto() {
        return new MatchDto(
                UUID.randomUUID(),
                "Fútbol",
                "LaLiga",
                "Real Madrid",
                "Barcelona",
                "REA",
                "BAR",
                0,
                0,
                1,
                "EN VIVO",
                "17:00",
                new OddsDto(new BigDecimal("1.80"), new BigDecimal("3.50"), new BigDecimal("4.20")),
                true,
                "ADMIN",
                true
        );
    }
}
