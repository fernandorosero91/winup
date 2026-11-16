package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchSchedulerTest {
    private static final Instant NOW = Instant.parse("2026-11-12T22:00:00Z");

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private GeneratorLock generatorLock;

    @Test
    void should_start_upcoming_matches_whose_time_has_come() {
        MatchEntity due = match(true);
        when(matchRepository.findByStatusAndMinuteAndStartsAtBefore("OPEN", 0, NOW))
                .thenReturn(List.of(due));

        scheduler().startDue();

        assertThat(due.getMinute()).isEqualTo(1);
        verify(matchRepository).save(due);
        verify(eventPublisher).scheduleSimulation(due);
    }

    @Test
    void should_ignore_matches_without_simulation() {
        MatchEntity due = match(false);
        when(matchRepository.findByStatusAndMinuteAndStartsAtBefore(anyString(), anyInt(), any()))
                .thenReturn(List.of(due));

        scheduler().startDue();

        assertThat(due.getMinute()).isZero();
        verify(matchRepository, never()).save(any());
        verify(eventPublisher, never()).scheduleSimulation(any());
    }

    @Test
    void should_skip_the_scan_when_another_replica_holds_the_lock() {
        when(generatorLock.run(any())).thenReturn(false);

        scheduler().startDueMatches();

        verify(matchRepository, never()).findByStatusAndMinuteAndStartsAtBefore(anyString(), anyInt(), any());
    }

    @Test
    void should_start_every_due_match() {
        MatchEntity first = match(true);
        MatchEntity second = match(true);
        when(matchRepository.findByStatusAndMinuteAndStartsAtBefore(anyString(), anyInt(), any()))
                .thenReturn(List.of(first, second));

        scheduler().startDue();

        verify(eventPublisher, org.mockito.Mockito.times(2)).scheduleSimulation(any());
    }

    private MatchScheduler scheduler() {
        return new MatchScheduler(
                matchRepository,
                eventPublisher,
                generatorLock,
                Clock.fixed(NOW, ZoneOffset.UTC),
                true
        );
    }

    private MatchEntity match(boolean simulated) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport("Fútbol");
        match.setStatus("OPEN");
        match.setMinute(0);
        match.setSimulated(simulated);
        match.setStartsAt(NOW.minusSeconds(60));
        return match;
    }
}
