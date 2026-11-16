package com.winup.service;

import com.winup.dto.CreateMatchRequest;
import com.winup.dto.MatchDto;
import com.winup.dto.UpdateOddsRequest;
import com.winup.entity.MatchEntity;
import com.winup.mapper.MatchMapper;
import com.winup.model.MatchStatus;
import com.winup.model.SportCatalog;
import com.winup.model.vo.LeagueName;
import com.winup.model.vo.Odds;
import com.winup.model.vo.TeamName;
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
    private final MatchRepository matchRepository;
    private final EventPublisher eventPublisher;
    private final MatchMapper matchMapper;
    private final MatchOddsSnapshotService snapshotService;
    private final Clock clock;

    public AdminMatchService(
            MatchRepository matchRepository,
            EventPublisher eventPublisher,
            MatchMapper matchMapper,
            MatchOddsSnapshotService snapshotService,
            Clock clock
    ) {
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
        this.matchMapper = matchMapper;
        this.snapshotService = snapshotService;
        this.clock = clock;
    }

    @Transactional
    public MatchDto create(CreateMatchRequest request) {
        String sport = sport(request.sport());
        TeamName home = new TeamName(request.home());
        TeamName away = new TeamName(request.away());
        if (home.sameAs(away)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Los equipos deben ser distintos"
            );
        }
        BigDecimal local = new Odds(request.localOdds()).value();
        BigDecimal awayOdds = new Odds(request.awayOdds()).value();
        BigDecimal draw = drawOdds(sport, request.drawOdds());

        Instant now = clock.instant();
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setSport(sport);
        match.setLeague(new LeagueName(request.league()).orDefault(sport));
        match.setHomeTeam(home.value());
        match.setAwayTeam(away.value());
        match.setHomeShort(shortName(home.value()));
        match.setAwayShort(shortName(away.value()));
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setMinute(request.minute() == null || request.minute() < 0 ? 1 : request.minute());
        match.setStatus(MatchStatus.OPEN.code());
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
        match.setLocalOdds(new Odds(request.localOdds()).value());
        match.setAwayOdds(new Odds(request.awayOdds()).value());
        if (SportCatalog.hasDraw(match.getSport())) {
            match.setDrawOdds(new Odds(request.drawOdds()).value());
        }
        match.setUpdatedAt(clock.instant());
        MatchEntity saved = matchRepository.save(match);
        snapshotService.record(saved);
        return matchMapper.toDto(saved);
    }

    private String sport(String value) {
        String sport = SportCatalog.normalize(value);
        if (!SportCatalog.SPORTS.contains(sport)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Deporte no válido");
        }
        return sport;
    }

    private BigDecimal drawOdds(String sport, BigDecimal value) {
        if (SportCatalog.hasDraw(sport)) {
            return new Odds(value).value();
        }
        return value == null ? BigDecimal.ONE : new Odds(value).value();
    }

    private String shortName(String team) {
        String cleaned = team.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return cleaned.length() <= 3 ? cleaned : cleaned.substring(0, 3);
    }
}
