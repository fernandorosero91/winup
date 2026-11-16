package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.entity.MatchOddsSnapshotEntity;
import com.winup.repository.MatchOddsSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchOddsSnapshotServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Mock
    private MatchOddsSnapshotRepository repository;

    private MatchOddsSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new MatchOddsSnapshotService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void should_save_the_first_snapshot_of_a_match() {
        MatchEntity match = match("1.80", "3.40", "4.20");
        when(repository.findTopByMatchIdOrderByCapturedAtDesc(match.getId()))
                .thenReturn(Optional.empty());

        assertThat(service.record(match)).isTrue();

        ArgumentCaptor<MatchOddsSnapshotEntity> captor =
                ArgumentCaptor.forClass(MatchOddsSnapshotEntity.class);
        verify(repository).save(captor.capture());
        MatchOddsSnapshotEntity snapshot = captor.getValue();
        assertThat(snapshot.getMatchId()).isEqualTo(match.getId());
        assertThat(snapshot.getLocalOdds()).isEqualByComparingTo("1.80");
        assertThat(snapshot.getDrawOdds()).isEqualByComparingTo("3.40");
        assertThat(snapshot.getAwayOdds()).isEqualByComparingTo("4.20");
        assertThat(snapshot.getSource()).isEqualTo("API_FOOTBALL");
        assertThat(snapshot.getCapturedAt()).isEqualTo(NOW);
    }

    @Test
    void should_skip_a_snapshot_when_the_odds_did_not_change() {
        MatchEntity match = match("1.80", "3.40", "4.20");
        when(repository.findTopByMatchIdOrderByCapturedAtDesc(match.getId()))
                .thenReturn(Optional.of(snapshot("1.80", "3.40", "4.20")));

        assertThat(service.record(match)).isFalse();

        verify(repository, never()).save(any());
    }

    @Test
    void should_save_a_snapshot_when_the_odds_change() {
        MatchEntity match = match("1.65", "3.60", "4.80");
        when(repository.findTopByMatchIdOrderByCapturedAtDesc(match.getId()))
                .thenReturn(Optional.of(snapshot("1.80", "3.40", "4.20")));

        assertThat(service.record(match)).isTrue();

        verify(repository).save(any(MatchOddsSnapshotEntity.class));
    }

    @Test
    void should_skip_a_match_without_odds() {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());

        assertThat(service.record(match)).isFalse();

        verify(repository, never()).findTopByMatchIdOrderByCapturedAtDesc(any());
        verify(repository, never()).save(any());
    }

    private MatchEntity match(String local, String draw, String away) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSource("API_FOOTBALL");
        match.setLocalOdds(new BigDecimal(local));
        match.setDrawOdds(new BigDecimal(draw));
        match.setAwayOdds(new BigDecimal(away));
        return match;
    }

    private MatchOddsSnapshotEntity snapshot(String local, String draw, String away) {
        MatchOddsSnapshotEntity snapshot = new MatchOddsSnapshotEntity();
        snapshot.setLocalOdds(new BigDecimal(local));
        snapshot.setDrawOdds(new BigDecimal(draw));
        snapshot.setAwayOdds(new BigDecimal(away));
        return snapshot;
    }
}
