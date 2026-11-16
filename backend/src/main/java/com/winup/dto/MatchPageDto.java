package com.winup.dto;

import java.util.List;

public record MatchPageDto(
        List<MatchDto> items,
        int page,
        int size,
        long total,
        boolean hasMore
) {
}
