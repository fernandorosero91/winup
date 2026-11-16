package com.winup.service.provider.random;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import com.winup.repository.MatchRepository;
import com.winup.service.MatchFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomAdapterTest {
    @Mock
    private MatchRepository matchRepository;

    @Test
    void should_generate_three_hundred_simulated_matches_for_all_sports() {
        when(matchRepository.count()).thenReturn(0L);
        RandomAdapter adapter = new RandomAdapter(matchRepository, new MatchFactory(), 2026L, 300);

        int seeded = adapter.seed(300);

        ArgumentCaptor<List<MatchEntity>> captor = listCaptor();
        verify(matchRepository).saveAll(captor.capture());
        List<MatchEntity> matches = captor.getValue();

        assertThat(seeded).isEqualTo(300);
        assertThat(matches).hasSize(300);
        assertThat(matches).extracting(MatchEntity::getSport).containsAll(SportCatalog.SPORTS);
        assertThat(matches).allMatch(MatchEntity::isSimulated);
        assertThat(matches).allMatch(match -> "RANDOM".equals(match.getSource()));
        assertThat(matches).anyMatch(match -> "CLOSED".equals(match.getStatus()));
        assertThat(matches).anyMatch(match -> "OPEN".equals(match.getStatus()));
    }

    @Test
    void should_generate_the_same_catalog_with_the_same_seed() {
        when(matchRepository.count()).thenReturn(0L);
        RandomAdapter first = new RandomAdapter(matchRepository, new MatchFactory(), 2026L, 30);
        RandomAdapter second = new RandomAdapter(matchRepository, new MatchFactory(), 2026L, 30);

        first.seed(30);
        second.seed(30);

        ArgumentCaptor<List<MatchEntity>> captor = listCaptor();
        verify(matchRepository, org.mockito.Mockito.times(2)).saveAll(captor.capture());
        List<MatchEntity> firstBatch = captor.getAllValues().get(0);
        List<MatchEntity> secondBatch = captor.getAllValues().get(1);

        assertThat(teams(firstBatch)).isEqualTo(teams(secondBatch));
    }

    @Test
    void should_not_seed_when_database_already_has_enough_matches() {
        when(matchRepository.count()).thenReturn(300L);
        RandomAdapter adapter = new RandomAdapter(matchRepository, new MatchFactory(), 2026L, 300);

        int seeded = adapter.seed(300);

        assertThat(seeded).isZero();
        verify(matchRepository, never()).saveAll(anyList());
    }

    @Test
    void should_seed_extra_matches_even_when_database_has_data() {
        RandomAdapter adapter = new RandomAdapter(matchRepository, new MatchFactory(), 2026L, 300);

        int seeded = adapter.seedExtra(30);

        assertThat(seeded).isEqualTo(30);
        verify(matchRepository).saveAll(anyList());
    }

    private List<String> teams(List<MatchEntity> matches) {
        return matches.stream()
                .map(match -> match.getHomeTeam() + "|" + match.getAwayTeam())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<MatchEntity>> listCaptor() {
        return ArgumentCaptor.forClass((Class<List<MatchEntity>>) (Class<?>) List.class);
    }
}
