package com.winup.service;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MatchResultSimulator {
    public record Result(int homeScore, int awayScore, String winner) {
    }

    public Result simulate(String sport) {
        return simulate(sport, ThreadLocalRandom.current());
    }

    public Result simulate(String sport, Random random) {
        return switch (sport) {
            case "Baloncesto", "NBA" -> playTwoWay(80, 120, random);
            case "Tenis", "eSports", "Voleibol" -> playTwoWay(0, 3, random);
            case "Béisbol" -> playTwoWay(0, 10, random);
            case "Fórmula 1", "MMA" -> playTwoWay(0, 1, random);
            case "AFL" -> playThreeWay(60, 120, random);
            case "Rugby" -> playThreeWay(10, 40, random);
            case "Balonmano" -> playThreeWay(20, 35, random);
            case "Hockey" -> playThreeWay(0, 6, random);
            default -> playThreeWay(0, 4, random);
        };
    }

    private Result playTwoWay(int min, int max, Random random) {
        int homeScore = random.nextInt(min, max + 1);
        int awayScore = random.nextInt(min, max + 1);
        if (homeScore == awayScore) {
            if (homeScore == max) {
                homeScore = max - 1;
            } else {
                homeScore++;
            }
        }
        String winner = homeScore > awayScore ? "LOCAL" : "VISITANTE";
        return new Result(homeScore, awayScore, winner);
    }

    private Result playThreeWay(int min, int max, Random random) {
        int choice = random.nextInt(3);
        if (choice == 0) {
            int homeScore = random.nextInt(min + 1, max + 1);
            int awayScore = random.nextInt(min, homeScore);
            return new Result(homeScore, awayScore, "LOCAL");
        }
        if (choice == 1) {
            int score = random.nextInt(min, max + 1);
            return new Result(score, score, "EMPATE");
        }
        int awayScore = random.nextInt(min + 1, max + 1);
        int homeScore = random.nextInt(min, awayScore);
        return new Result(homeScore, awayScore, "VISITANTE");
    }
}
