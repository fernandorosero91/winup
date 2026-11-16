package com.winup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winup.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "winup.push.provider", havingValue = "fcm")
public class FcmMobilePushAdapter implements MobilePushPort {
    private static final Logger log = LoggerFactory.getLogger(FcmMobilePushAdapter.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final DeviceTokenRepository deviceTokenRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final HttpClient httpClient;
    private final String projectId;
    private final String clientEmail;
    private final String privateKey;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public FcmMobilePushAdapter(
            DeviceTokenRepository deviceTokenRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${winup.push.fcm.project-id:}") String projectId,
            @Value("${winup.push.fcm.client-email:}") String clientEmail,
            @Value("${winup.push.fcm.private-key:}") String privateKey
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.httpClient = HttpClient.newHttpClient();
        this.projectId = projectId;
        this.clientEmail = clientEmail;
        this.privateKey = privateKey;
    }

    @Override
    public void push(String userId, String title, String body) {
        if (missingConfiguration()) {
            log.warn("FCM no esta configurado; push para {} omitido", userId);
            return;
        }
        List<String> tokens = deviceTokenRepository.findByUserId(userId).stream()
                .map(device -> device.getToken())
                .toList();
        if (tokens.isEmpty()) {
            log.info("Sin dispositivos FCM registrados para {}", userId);
            return;
        }
        try {
            String bearer = accessToken();
            for (String token : tokens) {
                sendMessage(bearer, token, title, body);
            }
        } catch (Exception exception) {
            log.error("No se pudo enviar el push FCM a {}", userId, exception);
        }
    }

    private boolean missingConfiguration() {
        return projectId.isBlank() || clientEmail.isBlank() || privateKey.isBlank();
    }

    private void sendMessage(String bearer, String token, String title, String body) throws Exception {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        Map<String, Object> androidNotification = new LinkedHashMap<>();
        androidNotification.put("sound", "default");
        androidNotification.put("channel_id", "winup_notifications");
        Map<String, Object> android = new LinkedHashMap<>();
        android.put("priority", "HIGH");
        android.put("notification", androidNotification);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", token);
        message.put("notification", notification);
        message.put("android", android);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FCM_URL.formatted(projectId)))
                .header("Authorization", "Bearer " + bearer)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of("message", message))))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            log.warn("FCM respondio {} para un dispositivo de {}", response.statusCode(), token);
        }
    }

    private String accessToken() throws Exception {
        if (accessToken != null && clock.instant().isBefore(accessTokenExpiry)) {
            return accessToken;
        }
        String assertion = assertion();
        String form = "grant_type=" + URLEncoder.encode(
                "urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(assertion, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Google OAuth respondio " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        accessToken = json.get("access_token").asText();
        accessTokenExpiry = clock.instant().plusSeconds(json.path("expires_in").asLong(3300) - 60);
        return accessToken;
    }

    private String assertion() throws Exception {
        Instant now = clock.instant();
        Map<String, Object> header = Map.of("alg", "RS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", clientEmail);
        claims.put("scope", SCOPE);
        claims.put("aud", TOKEN_URL);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(3600).getEpochSecond());

        String unsigned = base64(header) + "." + base64(claims);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey());
        signature.update(unsigned.getBytes(StandardCharsets.UTF_8));
        return unsigned + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signature.sign());
    }

    private PrivateKey privateKey() throws Exception {
        String normalized = privateKey.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private String base64(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }
}
