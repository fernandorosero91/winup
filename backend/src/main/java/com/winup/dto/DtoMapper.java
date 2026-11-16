package com.winup.dto;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.entity.MovementEntity;
import com.winup.model.SportCatalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DtoMapper {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("America/Bogota"));

    private DtoMapper() {
    }

    public static MatchDto toMatch(MatchEntity match) {
        String status = match.getStatus().equals("CLOSED")
                ? "FINALIZADO"
                : match.getMinute() > 0 ? "EN VIVO" : "PRÓXIMO";

        return new MatchDto(
                match.getId(),
                match.getSport(),
                match.getLeague(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getHomeShort(),
                match.getAwayShort(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMinute(),
                status,
                TIME.format(match.getStartsAt()),
                new OddsDto(match.getLocalOdds(), match.getDrawOdds(), match.getAwayOdds()),
                SportCatalog.hasDraw(match.getSport()),
                match.getSource(),
                match.isSimulated()
        );
    }

    public static BetDto toBet(BetEntity bet, MatchEntity match) {
        BigDecimal potential = bet.getStake().multiply(bet.getOdds()).setScale(2, RoundingMode.HALF_EVEN);
        String status = switch (bet.getStatus()) {
            case "WON" -> "GANADA";
            case "LOST" -> "PERDIDA";
            default -> "PENDIENTE";
        };
        String matchName = match == null
                ? "Partido"
                : match.getHomeTeam() + " vs " + match.getAwayTeam();

        return new BetDto(
                bet.getId(),
                matchName,
                "Resultado final",
                bet.getSelection().equals("LOCAL")
                        ? match == null ? "Local" : match.getHomeTeam()
                        : bet.getSelection().equals("VISITANTE")
                        ? match == null ? "Visitante" : match.getAwayTeam()
                        : "Empate",
                bet.getStake(),
                bet.getOdds(),
                potential,
                status,
                TIME.format(bet.getCreatedAt())
        );
    }

    public static MovementDto toMovement(MovementEntity movement) {
        return new MovementDto(
                movement.getId(),
                movement.getConcept(),
                TIME.format(movement.getCreatedAt()),
                movement.getAmount(),
                movement.getType()
        );
    }
}
