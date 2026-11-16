package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import com.winup.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratorServiceTest {
    @Mock
    private MatchService matchService;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private GeneratorLock generatorLock;

    @Test
    void should_create_an_active_match_for_every_sport_when_missing() {
        allowLock();
        when(matchRepository.countBySportAndStatusAndMinuteGreaterThan(anyString(), eq("OPEN"), eq(0)))
                .thenReturn(0L);
        when(matchService.createForSport(anyString())).thenReturn(new MatchEntity());

        service(1, 100).ensureAllSports();

        verify(matchService, times(SportCatalog.SPORTS.size())).createForSport(anyString());
        verify(eventPublisher, times(SportCatalog.SPORTS.size())).matchCreated(any());
    }

    @Test
    void should_replenish_every_missing_match_in_a_single_tick() {
        allowLock();
        when(matchRepository.countBySportAndStatusAndMinuteGreaterThan(anyString(), eq("OPEN"), eq(0)))
                .thenReturn(0L);
        when(matchService.createForSport(anyString())).thenReturn(new MatchEntity());

        service(4, 28).tick();

        verify(matchService, times(28)).createForSport(anyString());
        verify(eventPublisher, times(28)).matchCreated(any());
    }

    @Test
    void should_not_create_matches_when_every_sport_is_full() {
        allowLock();
        when(matchRepository.countBySportAndStatusAndMinuteGreaterThan(anyString(), eq("OPEN"), eq(0)))
                .thenReturn(4L);

        service(4, 28).tick();

        verify(matchService, never()).createForSport(anyString());
        verify(eventPublisher, never()).matchCreated(any());
    }

    @Test
    void should_skip_generation_when_another_replica_holds_the_lock() {
        when(generatorLock.run(any())).thenReturn(false);

        service(4, 28).tick();

        verify(matchService, never()).createForSport(anyString());
    }

    private void allowLock() {
        when(generatorLock.run(any())).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return true;
        });
    }

    private GeneratorService service(int livePerSport, int maxPerTick) {
        return new GeneratorService(
                matchService,
                matchRepository,
                eventPublisher,
                generatorLock,
                true,
                livePerSport,
                maxPerTick
        );
    }
}
