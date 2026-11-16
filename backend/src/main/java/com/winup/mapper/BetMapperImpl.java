package com.winup.mapper;

import com.winup.dto.BetDto;
import com.winup.dto.BetOutcomeDto;
import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class BetMapperImpl implements BetMapper {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("America/Bogota"));

    @Override
    public BetDto toDto(BetEntity bet, MatchEntity match) {
        return new BetDto(
                bet.getId(),
                matchName(match),
                "Resultado final",
                selectionName(bet, match),
                bet.getStake(),
                bet.getOdds(),
                potential(bet),
                status(bet),
                TIME.format(bet.getCreatedAt()),
                outcome(match)
        );
    }

    private String matchName(MatchEntity match) {
        return match == null ? "Partido" : match.getHomeTeam() + " vs " + match.getAwayTeam();
    }

    private String selectionName(BetEntity bet, MatchEntity match) {
        if ("LOCAL".equals(bet.getSelection())) {
            return match == null ? "Local" : match.getHomeTeam();
        }
        if ("VISITANTE".equals(bet.getSelection())) {
            return match == null ? "Visitante" : match.getAwayTeam();
        }
        return "Empate";
    }

    private BigDecimal potential(BetEntity bet) {
        return bet.getStake().multiply(bet.getOdds()).setScale(2, RoundingMode.HALF_EVEN);
    }

    private String status(BetEntity bet) {
        return switch (bet.getStatus()) {
            case "WON" -> "GANADA";
            case "LOST" -> "PERDIDA";
            default -> "PENDIENTE";
        };
    }

    private BetOutcomeDto outcome(MatchEntity match) {
        if (match == null) {
            return new BetOutcomeDto(null, null, null, null, false);
        }
        String winner = match.getWinner();
        return new BetOutcomeDto(
                winner,
                winnerName(match, winner),
                match.getHomeScore(),
                match.getAwayScore(),
                "CLOSED".equals(match.getStatus())
        );
    }

    private String winnerName(MatchEntity match, String winner) {
        if (winner == null || winner.isBlank()) {
            return null;
        }
        return switch (winner) {
            case "LOCAL" -> match.getHomeTeam();
            case "VISITANTE" -> match.getAwayTeam();
            case "EMPATE" -> "Empate";
            default -> null;
        };
    }
}
