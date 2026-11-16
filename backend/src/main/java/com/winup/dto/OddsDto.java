package com.winup.dto;

import java.math.BigDecimal;

public record OddsDto(BigDecimal local, BigDecimal draw, BigDecimal away) {
}
