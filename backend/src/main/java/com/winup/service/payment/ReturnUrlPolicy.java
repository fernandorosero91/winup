package com.winup.service.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class ReturnUrlPolicy {
    private static final String RETURN_PATH = "/api/v1/payments/return";
    private static final String APP_TARGET_SUFFIX = "/payments/return";

    private final List<String> allowedOrigins;
    private final String fallback;
    private final String publicBase;
    private final String webReturnUrl;

    public ReturnUrlPolicy(
            @Value("${winup.payments.allowed-return-origins:http://localhost:4200,http://127.0.0.1:8080}")
            String allowedOrigins,
            @Value("${winup.payments.mercadopago.back-url:http://localhost:4200/cartera}")
            String fallback,
            @Value("${winup.public-url:http://localhost:4200}")
            String publicUrl,
            @Value("${winup.payments.web-return-url:http://localhost:4200/cartera}")
            String webReturnUrl
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .map(origin -> origin.toLowerCase(Locale.ROOT))
                .toList();
        this.fallback = fallback;
        this.publicBase = publicUrl.replaceAll("/+$", "");
        this.webReturnUrl = webReturnUrl;
    }

    public String resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            return fallback;
        }
        String candidate = requested.trim();
        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            if (scheme == null || authority == null) {
                return fallback;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                return fallback;
            }
            String origin = normalizedScheme + "://" + authority.toLowerCase(Locale.ROOT);
            return allowedOrigins.contains(origin) ? candidate : fallback;
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public String publicReturnUrl(String requested) {
        String target = resolve(requested);
        if (isPublicHttps(target)) {
            return target;
        }
        return publicBase + RETURN_PATH + (isAppTarget(target) ? "/app" : "/web");
    }

    public String webReturnUrl() {
        return webReturnUrl;
    }

    private boolean isPublicHttps(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            return !"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isAppTarget(String url) {
        try {
            String path = URI.create(url).getPath();
            return path != null && path.endsWith(APP_TARGET_SUFFIX);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
