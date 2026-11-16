package com.winup.service;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
public class GeneratorLock {
    private static final long LOCK_ID = 918273L;
    private static final String ACQUIRE = "select pg_try_advisory_lock(?)";
    private static final String RELEASE = "select pg_advisory_unlock(?)";

    private final JdbcTemplate jdbcTemplate;

    public GeneratorLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean run(Runnable task) {
        Boolean executed = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            if (!acquire(connection)) {
                return false;
            }
            try {
                task.run();
                return true;
            } finally {
                release(connection);
            }
        });
        return Boolean.TRUE.equals(executed);
    }

    private boolean acquire(Connection connection) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ACQUIRE)) {
            statement.setLong(1, LOCK_ID);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private void release(Connection connection) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(RELEASE)) {
            statement.setLong(1, LOCK_ID);
            statement.execute();
        }
    }
}
