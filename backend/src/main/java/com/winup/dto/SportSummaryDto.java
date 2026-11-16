package com.winup.dto;

public record SportSummaryDto(
        String name,
        long live,
        long upcoming,
        long finished,
        long total
) {
}
