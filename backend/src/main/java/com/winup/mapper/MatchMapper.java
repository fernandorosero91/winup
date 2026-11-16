package com.winup.mapper;

import com.winup.entity.MatchEntity;
import com.winup.dto.MatchDto;

public interface MatchMapper {
    MatchDto toDto(MatchEntity entity);
}
