package com.winup.dto;

public record BetOutcomeDto(
        String winner,
        String winnerName,
        Integer homeScore,
        Integer awayScore,
        boolean finished
) {
}
