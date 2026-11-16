package com.winup.mapper;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.dto.BetDto;
import com.winup.dto.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class BetMapperImpl implements BetMapper {
    @Override
    public BetDto toDto(BetEntity bet, MatchEntity match) {
        return DtoMapper.toBet(bet, match);
    }
}
