package com.winup.dto;

public record AdminDashboardDto(
        long users,
        long matches,
        long bets,
        long payments,
        String ledgerDebits,
        String ledgerCredits
) {
}
