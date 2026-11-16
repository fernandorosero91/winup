package com.winup.dto;

import com.winup.entity.MatchEntity;
import com.winup.entity.MovementEntity;
import com.winup.model.MatchStatus;
import com.winup.model.SportCatalog;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DtoMapper {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("America/Bogota"));

    private DtoMapper() {
    }

    public static MatchDto toMatch(MatchEntity match) {
        String status = MatchStatus.from(match.getStatus()).label(match.getMinute());

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
