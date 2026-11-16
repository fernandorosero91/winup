package com.winup.dto;

import java.util.List;

public record MovementPageDto(
        List<MovementDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
