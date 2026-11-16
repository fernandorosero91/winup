package com.winup.service;

import com.winup.entity.MovementEntity;
import com.winup.entity.UserEntity;
import com.winup.entity.WalletEntity;
import com.winup.model.vo.DisplayName;
import com.winup.model.vo.Email;
import com.winup.model.vo.Password;
import com.winup.model.vo.PasswordAttempt;
import com.winup.dto.AuthResponse;
import com.winup.dto.LoginRequest;
import com.winup.dto.RegisterRequest;
import com.winup.dto.UserDto;
import com.winup.repository.MovementRepository;
import com.winup.repository.UserRepository;
import com.winup.repository.WalletRepository;
import com.winup.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000.00");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MovementRepository movementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Email email = new Email(request.email());
        Password password = new Password(request.password());
        DisplayName displayName = new DisplayName(request.displayName());

        if (userRepository.existsByEmailIgnoreCase(email.value())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Instant now = clock.instant();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email.value());
        user.setPasswordHash(passwordEncoder.encode(password.value()));
        user.setDisplayName(displayName.value());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        userRepository.save(user);

        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(user.getId());
        wallet.setBalance(INITIAL_BALANCE.setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

        MovementEntity movement = new MovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setUserId(user.getId());
        movement.setConcept("Saldo inicial");
        movement.setAmount(INITIAL_BALANCE.setScale(2, RoundingMode.HALF_EVEN));
        movement.setType("INGRESO");
        movement.setCreatedAt(now);
        movementRepository.save(movement);

        return new AuthResponse(jwtService.issue(user), toDto(user));
    }

    public AuthResponse login(LoginRequest request) {
        Email email = new Email(request.email());
        PasswordAttempt password = new PasswordAttempt(request.password());

        UserEntity user = userRepository.findByEmailIgnoreCase(email.value())
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(password.value(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return new AuthResponse(jwtService.issue(user), toDto(user));
    }

    public UserDto me(String userId) {
        return userRepository.findById(userId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
    }

    private UserDto toDto(UserEntity user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }
}
