package com.winup.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SportCatalogTest {
    @Test
    void should_return_all_when_value_is_blank_or_all() {
        assertThat(SportCatalog.normalize(null)).isEqualTo("TODOS");
        assertThat(SportCatalog.normalize(" ")).isEqualTo("TODOS");
        assertThat(SportCatalog.normalize("todos")).isEqualTo("TODOS");
    }

    @Test
    void should_return_canonical_name_regardless_of_case() {
        assertThat(SportCatalog.normalize("fútbol")).isEqualTo("Fútbol");
        assertThat(SportCatalog.normalize("BALONCESTO")).isEqualTo("Baloncesto");
        assertThat(SportCatalog.normalize(" esports ")).isEqualTo("eSports");
    }

    @Test
    void should_keep_unknown_sport_trimmed() {
        assertThat(SportCatalog.normalize("  Hockey ")).isEqualTo("Hockey");
    }

    @Test
    void should_mark_only_draw_sports_as_draw_markets() {
        assertThat(SportCatalog.hasDraw("Fútbol")).isTrue();
        assertThat(SportCatalog.hasDraw("NBA")).isFalse();
        assertThat(SportCatalog.hasDraw("MMA")).isFalse();
        assertThat(SportCatalog.hasDraw("Voleibol")).isFalse();
    }
}
