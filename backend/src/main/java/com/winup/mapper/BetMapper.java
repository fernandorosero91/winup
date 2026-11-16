package com.winup.mapper;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.dto.BetDto;

public interface BetMapper {
    BetDto toDto(BetEntity bet, MatchEntity match);
}
