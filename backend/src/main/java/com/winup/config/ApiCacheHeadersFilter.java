package com.winup.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiCacheHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (HttpMethod.GET.matches(request.getMethod()) && path.equals("/api/v1/sports")) {
            response.setHeader("Cache-Control", "private, max-age=60, stale-while-revalidate=120");
        } else if (HttpMethod.GET.matches(request.getMethod())
                && (path.equals("/api/v1/matches") || path.startsWith("/api/v1/matches/"))) {
            response.setHeader("Cache-Control", "private, max-age=5, stale-while-revalidate=25");
        } else if (path.startsWith("/api/") || path.startsWith("/actuator/")) {
            response.setHeader("Cache-Control", "private, no-store");
        }
        filterChain.doFilter(request, response);
    }
}
