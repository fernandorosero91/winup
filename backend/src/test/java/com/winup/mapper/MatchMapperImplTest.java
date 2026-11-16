package com.winup.mapper;

import com.winup.entity.MatchEntity;
import com.winup.dto.MatchDto;
import com.winup.service.MatchFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchMapperImplTest {
    @Test
    void should_map_a_match_entity_to_a_dto() {
        MatchEntity entity = new MatchFactory().createForSport("Fútbol");
        MatchMapper mapper = new MatchMapperImpl();

        MatchDto dto = mapper.toDto(entity);

        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.sport()).isEqualTo("Fútbol");
        assertThat(dto.home()).isEqualTo(entity.getHomeTeam());
        assertThat(dto.isSimulated()).isTrue();
        assertThat(dto.source()).isEqualTo("RANDOM");
    }
}
