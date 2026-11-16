package com.winup.service.sports.quota;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "winup.sports.quota.store", havingValue = "jdbc", matchIfMissing = true)
public class JdbcQuotaStore implements QuotaStore {
    private static final String ACQUIRE = """
            insert into provider_quota
                (provider, quota_day, used_count, quota_limit, remaining, open_until, updated_at)
            values (?, ?, 1, ?, null, null, ?)
            on conflict (provider, quota_day) do update
            set used_count = provider_quota.used_count + 1,
                updated_at = excluded.updated_at
            where (provider_quota.open_until is null or provider_quota.open_until <= ?)
              and provider_quota.used_count < ceil(provider_quota.quota_limit * ? / 100.0)
            returning quota_limit, used_count, remaining, open_until
            """;

    private static final String RECORD_RESPONSE = """
            update provider_quota
            set quota_limit = coalesce(?, quota_limit),
                remaining = coalesce(?, remaining),
                used_count = case
                    when ? is null then used_count
                    else greatest(0, coalesce(?, quota_limit) - ?)
                end,
                updated_at = ?
            where provider = ? and quota_day = ?
            """;

    private static final String OPEN_CIRCUIT = """
            insert into provider_quota
                (provider, quota_day, used_count, quota_limit, remaining, open_until, updated_at)
            values (?, ?, 0, ?, null, ?, ?)
            on conflict (provider, quota_day) do update
            set open_until = excluded.open_until,
                updated_at = excluded.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcQuotaStore(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public Optional<QuotaSnapshot> acquire(String provider, LocalDate day, int defaultLimit, int stopPercent) {
        Instant now = clock.instant();
        return jdbcTemplate.query(
                ACQUIRE,
                (resultSet, row) -> new QuotaSnapshot(
                        resultSet.getInt("quota_limit"),
                        resultSet.getInt("used_count"),
                        resultSet.getObject("remaining", Integer.class),
                        resultSet.getTimestamp("open_until") == null
                                ? null
                                : resultSet.getTimestamp("open_until").toInstant()
                ),
                provider,
                day,
                defaultLimit,
                Timestamp.from(now),
                Timestamp.from(now),
                stopPercent
        ).stream().findFirst();
    }

    @Override
    public void recordResponse(String provider, LocalDate day, Integer limit, Integer remaining) {
        jdbcTemplate.update(
                RECORD_RESPONSE,
                limit,
                remaining,
                remaining,
                limit,
                remaining,
                Timestamp.from(clock.instant()),
                provider,
                day
        );
    }

    @Override
    public void openCircuit(String provider, LocalDate day, Instant until, int defaultLimit) {
        Instant now = clock.instant();
        jdbcTemplate.update(
                OPEN_CIRCUIT,
                provider,
                day,
                defaultLimit,
                Timestamp.from(until),
                Timestamp.from(now)
        );
    }

    @Override
    public void closeCircuit(String provider, LocalDate day) {
        jdbcTemplate.update(
                "update provider_quota set open_until = null, updated_at = ? where provider = ? and quota_day = ?",
                Timestamp.from(clock.instant()),
                provider,
                day
        );
    }

    @Override
    public QuotaSnapshot load(String provider, LocalDate day, int defaultLimit) {
        return jdbcTemplate.query(
                """
                select quota_limit, used_count, remaining, open_until
                from provider_quota
                where provider = ? and quota_day = ?
                """,
                (resultSet, row) -> new QuotaSnapshot(
                        resultSet.getInt("quota_limit"),
                        resultSet.getInt("used_count"),
                        resultSet.getObject("remaining", Integer.class),
                        resultSet.getTimestamp("open_until") == null
                                ? null
                                : resultSet.getTimestamp("open_until").toInstant()
                ),
                provider,
                day
        ).stream().findFirst().orElseGet(() -> new QuotaSnapshot(defaultLimit, 0, null, null));
    }
}
