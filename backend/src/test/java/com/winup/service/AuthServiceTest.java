package com.winup.service;

import com.winup.entity.MovementEntity;
import com.winup.entity.UserEntity;
import com.winup.entity.WalletEntity;
import com.winup.exception.ValidationException;
import com.winup.dto.AuthResponse;
import com.winup.dto.LoginRequest;
import com.winup.dto.RegisterRequest;
import com.winup.repository.MovementRepository;
import com.winup.repository.UserRepository;
import com.winup.repository.WalletRepository;
import com.winup.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-11T18:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void should_register_user_with_hashed_password_and_initial_wallet() {
        when(userRepository.existsByEmailIgnoreCase("alex@winup.app")).thenReturn(false);
        when(passwordEncoder.encode("Winup2026")).thenReturn("bcrypt-hash");
        when(jwtService.issue(any(UserEntity.class))).thenReturn("jwt-token");
        AuthService service = serviceWithClock();

        AuthResponse response = service.register(
                new RegisterRequest("Alex@Winup.App ", "Winup2026", " Alex ")
        );

        ArgumentCaptor<UserEntity> user = ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<WalletEntity> wallet = ArgumentCaptor.forClass(WalletEntity.class);
        ArgumentCaptor<MovementEntity> movement = ArgumentCaptor.forClass(MovementEntity.class);
        verify(userRepository).save(user.capture());
        verify(walletRepository).save(wallet.capture());
        verify(movementRepository).save(movement.capture());

        assertThat(user.getValue().getEmail()).isEqualTo("alex@winup.app");
        assertThat(user.getValue().getDisplayName()).isEqualTo("Alex");
        assertThat(user.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(wallet.getValue().getBalance()).isEqualByComparingTo("100000.00");
        assertThat(movement.getValue().getAmount()).isEqualByComparingTo("100000.00");
        assertThat(movement.getValue().getType()).isEqualTo("INGRESO");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().displayName()).isEqualTo("Alex");
    }

    @Test
    void should_reject_duplicate_email() {
        when(userRepository.existsByEmailIgnoreCase("alex@winup.app")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alex@winup.app", "Winup2026", "Alex")
        ))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void should_reject_invalid_email_on_register() {
        AuthService service = serviceWithClock();

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("no-es-correo", "Winup2026", "Alex")
        ))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_EMAIL"));
    }

    @Test
    void should_reject_weak_password_on_register() {
        AuthService service = serviceWithClock();

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("alex@winup.app", "12345678", "Alex")
        ))
                .isInstanceOfSatisfying(ValidationException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("WEAK_PASSWORD");
                    assertThat(exception.getMessage()).contains("letra");
                });
    }

    @Test
    void should_login_with_valid_credentials() {
        UserEntity user = user();
        when(userRepository.findByEmailIgnoreCase("alex@winup.app")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Winup2026", "hash")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("alex@winup.app", "Winup2026"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("alex@winup.app");
    }

    @Test
    void should_return_generic_unauthorized_for_invalid_password() {
        when(userRepository.findByEmailIgnoreCase("alex@winup.app")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alex@winup.app", "wrong")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Credenciales inválidas");
                });
    }

    @Test
    void should_return_unauthorized_when_user_does_not_exist() {
        when(userRepository.findByEmailIgnoreCase("missing@winup.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@winup.app", "Winup2026")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private AuthService serviceWithClock() {
        return new AuthService(
                userRepository,
                walletRepository,
                movementRepository,
                passwordEncoder,
                jwtService,
                clock
        );
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setEmail("alex@winup.app");
        user.setDisplayName("Alex");
        user.setPasswordHash("hash");
        return user;
    }
}
