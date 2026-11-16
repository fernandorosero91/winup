package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.SportCatalog;
import com.winup.dto.CreateMatchRequest;
import com.winup.dto.MatchDto;
import com.winup.dto.UpdateOddsRequest;
import com.winup.mapper.MatchMapper;
import com.winup.repository.MatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminMatchService {
    private static final BigDecimal MIN_ODDS = new BigDecimal("1.01");
    private static final BigDecimal MAX_ODDS = new BigDecimal("100.00");

    private final MatchRepository matchRepository;
    private final EventPublisher eventPublisher;
    private final MatchMapper matchMapper;
    private final Clock clock;

    public AdminMatchService(
            MatchRepository matchRepository,
            EventPublisher eventPublisher,
            MatchMapper matchMapper,
            Clock clock
    ) {
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
        this.matchMapper = matchMapper;
        this.clock = clock;
    }

    @Transactional
    public MatchDto create(CreateMatchRequest request) {
        String sport = SportCatalog.normalize(request.sport());
        if (!SportCatalog.SPORTS.contains(sport)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Deporte no válido");
        }
        String home = requireText(request.home(), "El equipo local es obligatorio");
        String away = requireText(request.away(), "El equipo visitante es obligatorio");
        if (home.equalsIgnoreCase(away)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Los equipos deben ser distintos");
        }
        BigDecimal local = odds(request.localOdds(), "La cuota local es obligatoria");
        BigDecimal awayOdds = odds(request.awayOdds(), "La cuota visitante es obligatoria");
        BigDecimal draw = SportCatalog.hasDraw(sport)
                ? odds(request.drawOdds(), "La cuota de empate es obligatoria")
                : request.drawOdds() == null ? BigDecimal.ONE : odds(request.drawOdds(), "Cuota inválida");

        Instant now = clock.instant();
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport(sport);
        match.setLeague(request.league() == null || request.league().isBlank() ? sport : request.league().trim());
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setHomeShort(shortName(home));
        match.setAwayShort(shortName(away));
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setMinute(request.minute() == null || request.minute() < 0 ? 1 : request.minute());
        match.setStatus("OPEN");
        match.setSource("ADMIN");
        match.setSimulated(true);
        match.setStartsAt(now);
        match.setLocalOdds(local);
        match.setDrawOdds(draw);
        match.setAwayOdds(awayOdds);
        match.setCreatedAt(now);
        match.setUpdatedAt(now);

        MatchEntity saved = matchRepository.save(match);
        eventPublisher.matchCreated(saved);
        return matchMapper.toDto(saved);
    }

    @Transactional
    public MatchDto updateOdds(UUID id, UpdateOddsRequest request) {
        MatchEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
        match.setLocalOdds(odds(request.localOdds(), "Cuota local inválida"));
        match.setAwayOdds(odds(request.awayOdds(), "Cuota visitante inválida"));
        if (SportCatalog.hasDraw(match.getSport())) {
            match.setDrawOdds(odds(request.drawOdds(), "Cuota de empate inválida"));
        }
        match.setUpdatedAt(clock.instant());
        return matchMapper.toDto(matchRepository.save(match));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return value.trim();
    }

    private BigDecimal odds(BigDecimal value, String message) {
        if (value == null || value.compareTo(MIN_ODDS) < 0 || value.compareTo(MAX_ODDS) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return value;
    }

    private String shortName(String team) {
        String cleaned = team.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return cleaned.length() <= 3 ? cleaned : cleaned.substring(0, 3);
    }
}
