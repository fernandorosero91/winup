package com.winup.service;

import com.winup.entity.MatchEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MatchFactory {
    private record Team(String name, String shortName) {
    }

    private record League(String sport, String name, List<Team> teams) {
    }

    private final List<League> leagues = List.of(
            new League("Fútbol", "Premier League", List.of(
                    new Team("Manchester City", "MCI"),
                    new Team("Liverpool", "LIV"),
                    new Team("Arsenal", "ARS"),
                    new Team("Chelsea", "CHE"),
                    new Team("Tottenham", "TOT"),
                    new Team("Manchester United", "MUN")
            )),
            new League("Fútbol", "LaLiga", List.of(
                    new Team("Real Madrid", "RMA"),
                    new Team("Barcelona", "BAR"),
                    new Team("Atlético de Madrid", "ATM"),
                    new Team("Sevilla", "SEV"),
                    new Team("Real Sociedad", "RSO"),
                    new Team("Villarreal", "VIL")
            )),
            new League("Fútbol", "Bundesliga", List.of(
                    new Team("Bayern Múnich", "BAY"),
                    new Team("Borussia Dortmund", "BVB"),
                    new Team("Leipzig", "RBL"),
                    new Team("Leverkusen", "B04"),
                    new Team("Eintracht Frankfurt", "SGE"),
                    new Team("Stuttgart", "VFB")
            )),
            new League("AFL", "AFL", List.of(
                    new Team("Carlton Blues", "CAR"),
                    new Team("Collingwood Magpies", "COL"),
                    new Team("Essendon Bombers", "ESS"),
                    new Team("Richmond Tigers", "RIC"),
                    new Team("Geelong Cats", "GEE"),
                    new Team("Sydney Swans", "SYD")
            )),
            new League("Béisbol", "MLB", List.of(
                    new Team("New York Yankees", "NYY"),
                    new Team("Boston Red Sox", "BOS"),
                    new Team("Los Angeles Dodgers", "LAD"),
                    new Team("San Francisco Giants", "SFG"),
                    new Team("Chicago Cubs", "CHC"),
                    new Team("Houston Astros", "HOU")
            )),
            new League("Baloncesto", "EuroLeague", List.of(
                    new Team("Real Madrid", "RMB"),
                    new Team("Barcelona", "FCB"),
                    new Team("Panathinaikos", "PAO"),
                    new Team("Olympiacos", "OLY"),
                    new Team("Baskonia", "BKN"),
                    new Team("Valencia Basket", "VAL")
            )),
            new League("Fórmula 1", "Fórmula 1", List.of(
                    new Team("Max Verstappen", "VER"),
                    new Team("Lando Norris", "NOR"),
                    new Team("Charles Leclerc", "LEC"),
                    new Team("Lewis Hamilton", "HAM"),
                    new Team("George Russell", "RUS"),
                    new Team("Fernando Alonso", "ALO")
            )),
            new League("Balonmano", "EHF Champions League", List.of(
                    new Team("Barcelona", "BAR"),
                    new Team("Veszprém", "VES"),
                    new Team("Kiel", "KIE"),
                    new Team("Paris Saint-Germain", "PSG"),
                    new Team("Aalborg", "AAL"),
                    new Team("Magdeburg", "MAG")
            )),
            new League("Hockey", "NHL", List.of(
                    new Team("Toronto Maple Leafs", "TOR"),
                    new Team("Montreal Canadiens", "MTL"),
                    new Team("Boston Bruins", "BOS"),
                    new Team("New York Rangers", "NYR"),
                    new Team("Edmonton Oilers", "EDM"),
                    new Team("Florida Panthers", "FLA")
            )),
            new League("MMA", "UFC", List.of(
                    new Team("Islam Makhachev", "MAK"),
                    new Team("Alex Pereira", "PER"),
                    new Team("Jon Jones", "JON"),
                    new Team("Tom Aspinall", "ASP"),
                    new Team("Ilia Topuria", "TOP"),
                    new Team("Max Holloway", "HOL")
            )),
            new League("NBA", "NBA", List.of(
                    new Team("Los Angeles Lakers", "LAL"),
                    new Team("Boston Celtics", "BOS"),
                    new Team("Golden State Warriors", "GSW"),
                    new Team("Miami Heat", "MIA"),
                    new Team("Milwaukee Bucks", "MIL"),
                    new Team("Denver Nuggets", "DEN")
            )),
            new League("NFL & NCAA", "NFL", List.of(
                    new Team("Kansas City Chiefs", "KC"),
                    new Team("San Francisco 49ers", "SF"),
                    new Team("Philadelphia Eagles", "PHI"),
                    new Team("Dallas Cowboys", "DAL"),
                    new Team("Buffalo Bills", "BUF"),
                    new Team("Detroit Lions", "DET")
            )),
            new League("NFL & NCAA", "NCAA", List.of(
                    new Team("Alabama Crimson Tide", "ALA"),
                    new Team("Georgia Bulldogs", "UGA"),
                    new Team("Ohio State Buckeyes", "OSU"),
                    new Team("Michigan Wolverines", "MICH"),
                    new Team("Texas Longhorns", "TEX"),
                    new Team("Oregon Ducks", "ORE")
            )),
            new League("Rugby", "Six Nations", List.of(
                    new Team("England", "ENG"),
                    new Team("France", "FRA"),
                    new Team("Ireland", "IRE"),
                    new Team("Wales", "WAL"),
                    new Team("Scotland", "SCO"),
                    new Team("Italy", "ITA")
            )),
            new League("Voleibol", "SuperLega", List.of(
                    new Team("Perugia", "PER"),
                    new Team("Modena", "MOD"),
                    new Team("Trentino", "TRE"),
                    new Team("Civitanova", "CIV"),
                    new Team("Piacenza", "PIA"),
                    new Team("Milano", "MIL")
            )),
            new League("Tenis", "ATP Masters", List.of(
                    new Team("Carlos Alcaraz", "ALC"),
                    new Team("Jannik Sinner", "SIN"),
                    new Team("Novak Djokovic", "DJO"),
                    new Team("Daniil Medvedev", "MED"),
                    new Team("Alexander Zverev", "ZVE"),
                    new Team("Holger Rune", "RUN")
            )),
            new League("eSports", "LoL Championship", List.of(
                    new Team("G2 Esports", "G2"),
                    new Team("Fnatic", "FNC"),
                    new Team("T1", "T1"),
                    new Team("Gen.G", "GEN"),
                    new Team("MAD Lions", "MAD"),
                    new Team("Team Heretics", "TH")
            ))
    );

    public MatchEntity create() {
        return create(ThreadLocalRandom.current());
    }

    public MatchEntity create(Random random) {
        League league = leagues.get(random.nextInt(leagues.size()));
        return createFrom(league, random);
    }

    public MatchEntity createForSport(String sport) {
        return createForSport(sport, ThreadLocalRandom.current());
    }

    public MatchEntity createForSport(String sport, Random random) {
        List<League> candidates = leagues.stream()
                .filter(league -> league.sport().equals(sport))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Deporte sin liga: " + sport);
        }
        MatchEntity match = createFrom(candidates.get(random.nextInt(candidates.size())), random);
        match.setMinute(1);
        return match;
    }

    private MatchEntity createFrom(League league, Random random) {
        int homeIndex = random.nextInt(league.teams().size());
        int awayIndex = random.nextInt(league.teams().size());
        while (awayIndex == homeIndex) {
            awayIndex = random.nextInt(league.teams().size());
        }

        Team home = league.teams().get(homeIndex);
        Team away = league.teams().get(awayIndex);
        BigDecimal localOdds = odds(random, 1.55, 2.60);
        BigDecimal drawOdds = odds(random, 3.10, 3.90);
        BigDecimal awayOdds = odds(random, 2.20, 4.60);

        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport(league.sport());
        match.setLeague(league.name());
        match.setHomeTeam(home.name());
        match.setAwayTeam(away.name());
        match.setHomeShort(home.shortName());
        match.setAwayShort(away.shortName());
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setMinute(random.nextInt(0, 2));
        match.setStatus("OPEN");
        match.setSource("RANDOM");
        match.setSimulated(true);
        match.setStartsAt(Instant.now());
        match.setLocalOdds(localOdds);
        match.setDrawOdds(drawOdds);
        match.setAwayOdds(awayOdds);
        match.setCreatedAt(Instant.now());
        match.setUpdatedAt(Instant.now());
        return match;
    }

    private BigDecimal odds(Random random, double min, double max) {
        double value = random.nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN);
    }
}
