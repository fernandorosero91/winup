package com.winup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "match_odds_snapshot",
        indexes = @Index(
                name = "idx_match_odds_snapshot_match",
                columnList = "match_id, captured_at"
        )
)
public class MatchOddsSnapshotEntity {
    @Id
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "local_odds", nullable = false, precision = 7, scale = 2)
    private BigDecimal localOdds;

    @Column(name = "draw_odds", precision = 7, scale = 2)
    private BigDecimal drawOdds;

    @Column(name = "away_odds", nullable = false, precision = 7, scale = 2)
    private BigDecimal awayOdds;

    @Column(nullable = false)
    private String source;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public BigDecimal getLocalOdds() {
        return localOdds;
    }

    public void setLocalOdds(BigDecimal localOdds) {
        this.localOdds = localOdds;
    }

    public BigDecimal getDrawOdds() {
        return drawOdds;
    }

    public void setDrawOdds(BigDecimal drawOdds) {
        this.drawOdds = drawOdds;
    }

    public BigDecimal getAwayOdds() {
        return awayOdds;
    }

    public void setAwayOdds(BigDecimal awayOdds) {
        this.awayOdds = awayOdds;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
}
