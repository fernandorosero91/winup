package com.winup.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class LeagueCatalog {
    public record Team(String name, String shortName) {
    }

    public record League(String sport, String name, List<Team> teams) {
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

    public League random(Random random) {
        return leagues.get(random.nextInt(leagues.size()));
    }

    public League forSport(String sport, Random random) {
        List<League> candidates = leagues.stream()
                .filter(league -> league.sport().equals(sport))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Deporte sin liga: " + sport);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}
