package com.winup.mapper;

import com.winup.entity.MatchEntity;
import com.winup.dto.DtoMapper;
import com.winup.dto.MatchDto;
import org.springframework.stereotype.Component;

@Component
public class MatchMapperImpl implements MatchMapper {
    @Override
    public MatchDto toDto(MatchEntity entity) {
        return DtoMapper.toMatch(entity);
    }
}
