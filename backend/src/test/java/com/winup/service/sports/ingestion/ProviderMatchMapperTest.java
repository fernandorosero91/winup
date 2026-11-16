package com.winup.service.sports.ingestion;

import com.winup.entity.MatchEntity;
import com.winup.service.sports.FixtureStatus;
import com.winup.service.sports.ProviderFixture;
import com.winup.service.sports.ProviderOdds;
import com.winup.service.sports.ProviderScore;
import com.winup.service.sports.ProviderTeam;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderMatchMapperTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private final ProviderMatchMapper mapper = new ProviderMatchMapper();

    @Test
    void should_create_a_new_match_from_a_fixture() {
        ProviderFixture fixture = fixture(
                FixtureStatus.SCHEDULED,
                "Real Madrid",
                "RMA",
                "Barcelona",
                "BAR",
                0,
                0,
                odds("2.10", "3.40", "3.20")
        );

        MatchEntity match = mapper.map(fixture, "REPLAY", NOW, null, odds("2.10", "3.40", "3.20"));

        assertThat(match.getId()).isNotNull();
        assertThat(match.getCreatedAt()).isEqualTo(NOW);
        assertThat(match.getSport()).isEqualTo("Fútbol");
        assertThat(match.getLeague()).isEqualTo("LaLiga");
        assertThat(match.getHomeTeam()).isEqualTo("Real Madrid");
        assertThat(match.getAwayTeam()).isEqualTo("Barcelona");
        assertThat(match.getHomeShort()).isEqualTo("RMA");
        assertThat(match.getAwayShort()).isEqualTo("BAR");
        assertThat(match.getStatus()).isEqualTo("OPEN");
        assertThat(match.getSource()).isEqualTo("REPLAY");
        assertThat(match.isSimulated()).isFalse();
        assertThat(match.getLocalOdds()).isEqualByComparingTo("2.10");
        assertThat(match.getDrawOdds()).isEqualByComparingTo("3.40");
        assertThat(match.getAwayOdds()).isEqualByComparingTo("3.20");
    }

    @Test
    void should_update_an_existing_match_and_keep_its_odds() {
        MatchEntity existing = new MatchEntity();
        existing.setLocalOdds(new BigDecimal("1.90"));
        existing.setDrawOdds(new BigDecimal("3.50"));
        existing.setAwayOdds(new BigDecimal("4.10"));
        ProviderFixture fixture = fixture(
                FixtureStatus.LIVE,
                "Manchester City",
                "MCI",
                "Liverpool",
                "LIV",
                1,
                0,
                null
        );

        MatchEntity match = mapper.map(fixture, "REPLAY", NOW, existing, null);

        assertThat(match).isSameAs(existing);
        assertThat(match.getHomeScore()).isEqualTo(1);
        assertThat(match.getMinute()).isEqualTo(65);
        assertThat(match.getLocalOdds()).isEqualByComparingTo("1.90");
        assertThat(match.getDrawOdds()).isEqualByComparingTo("3.50");
        assertThat(match.getAwayOdds()).isEqualByComparingTo("4.10");
    }

    @Test
    void should_fill_the_draw_odds_with_zero_when_missing() {
        ProviderFixture fixture = fixture(
                FixtureStatus.SCHEDULED,
                "Lakers",
                "LAL",
                "Celtics",
                "BOS",
                0,
                0,
                odds("2.05", null, "1.80")
        );

        MatchEntity match = mapper.map(fixture, "REPLAY", NOW, null, odds("2.05", null, "1.80"));

        assertThat(match.getDrawOdds()).isEqualByComparingTo("0.00");
    }

    @Test
    void should_derive_the_winner_from_the_score() {
        assertThat(mapper.winner(fixture(FixtureStatus.FINISHED, "A", "A", "B", "B", 2, 1, null)))
                .isEqualTo("LOCAL");
        assertThat(mapper.winner(fixture(FixtureStatus.FINISHED, "A", "A", "B", "B", 1, 1, null)))
                .isEqualTo("EMPATE");
        assertThat(mapper.winner(fixture(FixtureStatus.FINISHED, "A", "A", "B", "B", 0, 3, null)))
                .isEqualTo("VISITANTE");
    }

    @Test
    void should_return_no_winner_for_a_tie_without_draw_market() {
        ProviderFixture fixture = new ProviderFixture(
                "tennis-1",
                "Tenis",
                "ATP",
                new ProviderTeam("Alcaraz", "ALC"),
                new ProviderTeam("Sinner", "SIN"),
                NOW,
                FixtureStatus.FINISHED,
                new ProviderScore(1, 1, 90, "FT"),
                odds("1.80", null, "2.00")
        );

        assertThat(mapper.winner(fixture)).isNull();
    }

    @Test
    void should_build_a_short_name_when_the_provider_has_none() {
        ProviderFixture fixture = fixture(
                FixtureStatus.SCHEDULED,
                "Barcelona",
                "",
                "Real Madrid",
                "",
                0,
                0,
                odds("2.00", "3.00", "3.50")
        );

        MatchEntity match = mapper.map(fixture, "REPLAY", NOW, null, odds("2.10", "3.40", "3.20"));

        assertThat(match.getHomeShort()).isEqualTo("BAR");
        assertThat(match.getAwayShort()).isEqualTo("REA");
    }

    private ProviderFixture fixture(
            FixtureStatus status,
            String home,
            String homeShort,
            String away,
            String awayShort,
            int homeScore,
            int awayScore,
            ProviderOdds odds
    ) {
        return new ProviderFixture(
                "fixture-1",
                "Fútbol",
                "LaLiga",
                new ProviderTeam(home, homeShort),
                new ProviderTeam(away, awayShort),
                Instant.parse("2026-11-09T20:00:00Z"),
                status,
                new ProviderScore(
                        homeScore,
                        awayScore,
                        status == FixtureStatus.LIVE ? 65 : status == FixtureStatus.FINISHED ? 90 : 0,
                        status == FixtureStatus.SCHEDULED ? "NS" : "FT"
                ),
                odds
        );
    }

    private ProviderOdds odds(String local, String draw, String away) {
        return new ProviderOdds(
                new BigDecimal(local),
                draw == null ? null : new BigDecimal(draw),
                new BigDecimal(away)
        );
    }
}
