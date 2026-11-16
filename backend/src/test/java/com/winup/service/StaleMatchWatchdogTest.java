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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleMatchWatchdogTest {
    private static final Instant NOW = Instant.parse("2026-11-12T22:00:00Z");

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultSimulator resultSimulator;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private GeneratorLock generatorLock;

    @Test
    void should_finish_simulated_matches_stuck_beyond_the_grace_period() {
        MatchEntity stale = match(true);
        when(matchRepository.findByStatusAndMinuteGreaterThanAndCreatedAtBefore(
                eq("OPEN"),
                eq(0),
                eq(NOW.minusMillis(180000))
        )).thenReturn(List.of(stale));
        when(resultSimulator.simulate(stale.getSport()))
                .thenReturn(new MatchResultSimulator.Result(2, 1, "LOCAL"));

        watchdog().closeStaleMatches();

        verify(eventPublisher).matchFinished(stale, "LOCAL", 2, 1);
    }

    @Test
    void should_leave_matches_without_simulation_untouched() {
        MatchEntity stale = match(false);
        when(matchRepository.findByStatusAndMinuteGreaterThanAndCreatedAtBefore(anyString(), anyInt(), any()))
                .thenReturn(List.of(stale));

        watchdog().closeStaleMatches();

        verify(resultSimulator, never()).simulate(anyString());
        verify(eventPublisher, never()).matchFinished(any(), anyString(), anyInt(), anyInt());
    }

    @Test
    void should_skip_the_sweep_when_another_replica_holds_the_lock() {
        when(generatorLock.run(any())).thenReturn(false);

        watchdog().sweep();

        verify(matchRepository, never())
                .findByStatusAndMinuteGreaterThanAndCreatedAtBefore(anyString(), anyInt(), any());
    }

    @Test
    void should_finish_every_stale_match_of_the_same_sweep() {
        MatchEntity first = match(true);
        MatchEntity second = match(true);
        when(matchRepository.findByStatusAndMinuteGreaterThanAndCreatedAtBefore(anyString(), anyInt(), any()))
                .thenReturn(List.of(first, second));
        when(resultSimulator.simulate(anyString()))
                .thenReturn(new MatchResultSimulator.Result(3, 0, "LOCAL"));

        watchdog().closeStaleMatches();

        verify(eventPublisher, times(2)).matchFinished(any(), eq("LOCAL"), eq(3), eq(0));
    }

    private void allowLock() {
        when(generatorLock.run(any())).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return true;
        });
    }

    private StaleMatchWatchdog watchdog() {
        return new StaleMatchWatchdog(
                matchRepository,
                resultSimulator,
                eventPublisher,
                generatorLock,
                Clock.fixed(NOW, ZoneOffset.UTC),
                true,
                180000
        );
    }

    private MatchEntity match(boolean simulated) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport("Fútbol");
        match.setStatus("OPEN");
        match.setMinute(1);
        match.setSimulated(simulated);
        return match;
    }
}
