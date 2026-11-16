package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MatchResultSimulator {
    public record Result(int homeScore, int awayScore, String winner) {
    }

    private record Range(int min, int max) {
    }

    private record WeightedOutcome(String outcome, double weight) {
    }

    public Result simulate(String sport) {
        return simulate(sport, null, null, null, ThreadLocalRandom.current());
    }

    public Result simulate(MatchEntity match) {
        return simulate(
                match.getSport(),
                match.getLocalOdds(),
                match.getDrawOdds(),
                match.getAwayOdds(),
                ThreadLocalRandom.current()
        );
    }

    public Result simulate(String sport, Random random) {
        return simulate(sport, null, null, null, random);
    }

    public Result simulate(
            String sport,
            BigDecimal localOdds,
            BigDecimal drawOdds,
            BigDecimal awayOdds,
            Random random
    ) {
        Range range = range(sport);
        String winner = chooseOutcome(
                SportCatalog.hasDraw(sport),
                localOdds,
                drawOdds,
                awayOdds,
                random
        );
        return switch (winner) {
            case "LOCAL" -> localWins(range, random);
            case "EMPATE" -> draw(range, random);
            default -> awayWins(range, random);
        };
    }

    private String chooseOutcome(
            boolean drawAllowed,
            BigDecimal localOdds,
            BigDecimal drawOdds,
            BigDecimal awayOdds,
            Random random
    ) {
        List<WeightedOutcome> options = new ArrayList<>();
        options.add(new WeightedOutcome("LOCAL", impliedProbability(localOdds)));
        if (drawAllowed) {
            options.add(new WeightedOutcome("EMPATE", impliedProbability(drawOdds)));
        }
        options.add(new WeightedOutcome("VISITANTE", impliedProbability(awayOdds)));

        double total = options.stream().mapToDouble(WeightedOutcome::weight).sum();
        if (total <= 0) {
            return options.get(random.nextInt(options.size())).outcome();
        }
        double target = random.nextDouble(total);
        double cumulative = 0;
        for (WeightedOutcome option : options) {
            cumulative += option.weight();
            if (target < cumulative) {
                return option.outcome();
            }
        }
        return options.get(options.size() - 1).outcome();
    }

    private double impliedProbability(BigDecimal odds) {
        if (odds == null) {
            return 0;
        }
        double value = odds.doubleValue();
        return value >= 1 ? 1 / value : 0;
    }

    private Result localWins(Range range, Random random) {
        int homeScore = random.nextInt(range.min() + 1, range.max() + 1);
        int awayScore = random.nextInt(range.min(), homeScore);
        return new Result(homeScore, awayScore, "LOCAL");
    }

    private Result awayWins(Range range, Random random) {
        int awayScore = random.nextInt(range.min() + 1, range.max() + 1);
        int homeScore = random.nextInt(range.min(), awayScore);
        return new Result(homeScore, awayScore, "VISITANTE");
    }

    private Result draw(Range range, Random random) {
        int score = random.nextInt(range.min(), range.max() + 1);
        return new Result(score, score, "EMPATE");
    }

    private Range range(String sport) {
        return switch (sport) {
            case "Baloncesto", "NBA" -> new Range(80, 120);
            case "Tenis", "eSports", "Voleibol" -> new Range(0, 3);
            case "Béisbol" -> new Range(0, 10);
            case "Fórmula 1", "MMA" -> new Range(0, 1);
            case "AFL" -> new Range(60, 120);
            case "Rugby" -> new Range(10, 40);
            case "Balonmano" -> new Range(20, 35);
            case "Hockey" -> new Range(0, 6);
            default -> new Range(0, 4);
        };
    }
}
