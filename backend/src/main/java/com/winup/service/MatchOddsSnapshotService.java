package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.entity.MatchOddsSnapshotEntity;
import com.winup.repository.MatchOddsSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchOddsSnapshotService {
    private final MatchOddsSnapshotRepository repository;
    private final Clock clock;

    public MatchOddsSnapshotService(MatchOddsSnapshotRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public boolean record(MatchEntity match) {
        if (match == null || match.getLocalOdds() == null || match.getAwayOdds() == null) {
            return false;
        }
        Optional<MatchOddsSnapshotEntity> latest =
                repository.findTopByMatchIdOrderByCapturedAtDesc(match.getId());
        if (latest.isPresent() && unchanged(latest.get(), match)) {
            return false;
        }

        MatchOddsSnapshotEntity snapshot = new MatchOddsSnapshotEntity();
        snapshot.setId(UUID.randomUUID());
        snapshot.setMatchId(match.getId());
        snapshot.setLocalOdds(match.getLocalOdds());
        snapshot.setDrawOdds(match.getDrawOdds());
        snapshot.setAwayOdds(match.getAwayOdds());
        snapshot.setSource(match.getSource());
        snapshot.setCapturedAt(clock.instant());
        repository.save(snapshot);
        return true;
    }

    private boolean unchanged(MatchOddsSnapshotEntity snapshot, MatchEntity match) {
        return same(snapshot.getLocalOdds(), match.getLocalOdds())
                && same(snapshot.getDrawOdds(), match.getDrawOdds())
                && same(snapshot.getAwayOdds(), match.getAwayOdds());
    }

    private boolean same(BigDecimal current, BigDecimal next) {
        if (current == null || next == null) {
            return current == null && next == null;
        }
        return current.compareTo(next) == 0;
    }
}
