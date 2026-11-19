package com.winup.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCacheHeadersFilterTest {
    private final ApiCacheHeadersFilter filter = new ApiCacheHeadersFilter();

    @Test
    void should_cache_matches_for_a_short_window() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/matches");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("private, max-age=5, stale-while-revalidate=25");
    }

    @Test
    void should_not_cache_wallet_data() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallet/summary");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Cache-Control")).isEqualTo("private, no-store");
    }
}
