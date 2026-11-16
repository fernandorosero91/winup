package com.winup.service;

import com.winup.entity.UserEntity;
import com.winup.dto.AdminDashboardDto;
import com.winup.dto.AdminUserDto;
import com.winup.service.provider.random.RandomAdapter;
import com.winup.mapper.MatchMapper;
import com.winup.repository.BetRepository;
import com.winup.repository.LedgerEntryRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.PaymentIntentRepository;
import com.winup.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private BetRepository betRepository;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private GeneratorService generatorService;

    @Mock
    private RandomAdapter randomAdapter;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AdminService adminService;

    @Test
    void should_publish_a_promotion_through_rabbit() {
        adminService.promote("Bono", "Deposita y recibe un bono");

        org.mockito.Mockito.verify(eventPublisher).promotion("Bono", "Deposita y recibe un bono");
    }

    @Test
    void should_report_dashboard_totals_and_ledger_sums() {
        when(userRepository.count()).thenReturn(3L);
        when(matchRepository.count()).thenReturn(20L);
        when(betRepository.count()).thenReturn(5L);
        when(paymentIntentRepository.count()).thenReturn(2L);
        when(ledgerEntryRepository.sumDebits()).thenReturn(new BigDecimal("50000.00"));
        when(ledgerEntryRepository.sumCredits()).thenReturn(new BigDecimal("50000.00"));

        AdminDashboardDto dashboard = adminService.dashboard();

        assertThat(dashboard.users()).isEqualTo(3);
        assertThat(dashboard.matches()).isEqualTo(20);
        assertThat(dashboard.ledgerDebits()).isEqualTo("50000.00");
        assertThat(dashboard.ledgerCredits()).isEqualTo("50000.00");
    }

    @Test
    void should_update_user_role() {
        UserEntity user = user("USER");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        AdminUserDto updated = adminService.updateRole("user-1", "admin");

        assertThat(updated.role()).isEqualTo("ADMIN");
        assertThat(user.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void should_reject_an_unknown_role() {
        assertThatThrownBy(() -> adminService.updateRole("user-1", "SUPERUSER"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private UserEntity user(String role) {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setEmail("user@winup.app");
        user.setDisplayName("User");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.parse("2026-11-12T18:00:00Z"));
        return user;
    }
}
