package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.model.MatchStatus;
import com.winup.model.SportCatalog;
import com.winup.dto.DtoMapper;
import com.winup.dto.MatchPageDto;
import com.winup.dto.SportSummaryDto;
import com.winup.repository.MatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final MatchFactory matchFactory;

    public MatchService(MatchRepository matchRepository, MatchFactory matchFactory) {
        this.matchRepository = matchRepository;
        this.matchFactory = matchFactory;
    }

    @Transactional
    public MatchEntity create() {
        return matchRepository.save(matchFactory.create());
    }

    @Transactional
    public MatchEntity createForSport(String sport) {
        return matchRepository.save(matchFactory.createForSport(sport));
    }

    public List<MatchEntity> all() {
        return matchRepository.findAllByOrderByCreatedAtDesc();
    }

    public MatchPageDto page(String status, String sport, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MatchEntity> result = matchRepository.search(
                normalizeStatus(status),
                SportCatalog.normalize(sport),
                pageable
        );
        return new MatchPageDto(
                result.getContent().stream().map(DtoMapper::toMatch).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.hasNext()
        );
    }

    public List<SportSummaryDto> sports() {
        Map<String, MatchRepository.SportCount> counts = matchRepository
                .countBySportStatus(MatchStatus.OPEN.code(), MatchStatus.CLOSED.code())
                .stream()
                .collect(Collectors.toMap(MatchRepository.SportCount::getSport, count -> count));
        return SportCatalog.SPORTS.stream()
                .map(sport -> summary(sport, counts.get(sport)))
                .toList();
    }

    private SportSummaryDto summary(String sport, MatchRepository.SportCount count) {
        if (count == null) {
            return new SportSummaryDto(sport, 0, 0, 0, 0);
        }
        return new SportSummaryDto(
                sport,
                count.getLive() == null ? 0 : count.getLive(),
                count.getUpcoming() == null ? 0 : count.getUpcoming(),
                count.getFinished() == null ? 0 : count.getFinished(),
                count.getTotal() == null ? 0 : count.getTotal()
        );
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status)) {
            return "TODOS";
        }
        if ("EN VIVO".equalsIgnoreCase(status) || "LIVE".equalsIgnoreCase(status)) {
            return "EN VIVO";
        }
        if ("PRÓXIMO".equalsIgnoreCase(status)
                || "PROXIMO".equalsIgnoreCase(status)
                || "UPCOMING".equalsIgnoreCase(status)) {
            return "PRÓXIMO";
        }
        if ("FINALIZADO".equalsIgnoreCase(status)
                || "FINISHED".equalsIgnoreCase(status)
                || MatchStatus.CLOSED.code().equalsIgnoreCase(status)) {
            return "FINALIZADO";
        }
        if ("ANULADO".equalsIgnoreCase(status) || MatchStatus.VOID.code().equalsIgnoreCase(status)) {
            return "ANULADO";
        }
        return "TODOS";
    }

    public Optional<MatchEntity> find(UUID id) {
        return matchRepository.findById(id);
    }

    public long activeCount() {
        return matchRepository.countByStatus(MatchStatus.OPEN.code());
    }

    @Transactional
    public MatchEntity finish(UUID id, String winner, int homeScore, int awayScore) {
        MatchEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        MatchStatus current = MatchStatus.from(match.getStatus());
        if (current == MatchStatus.CLOSED || current == MatchStatus.VOID) {
            return match;
        }
        match.setStatus(MatchStatus.CLOSED.code());
        match.setWinner(winner);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setMinute(90);
        match.setUpdatedAt(Instant.now());
        return matchRepository.save(match);
    }
}
