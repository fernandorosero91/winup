package com.winup.service;

import com.winup.entity.MatchEntity;
import com.winup.entity.WalletEntity;
import com.winup.exception.ValidationException;
import com.winup.dto.PlaceBetRequest;
import com.winup.dto.WalletDto;
import com.winup.mapper.BetMapper;
import com.winup.repository.BetRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetServiceTest {
    private static final String USER = "user-1";
    private static final String KEY = "idem-1";
    private static final UUID MATCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private BetRepository betRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private BetMapper betMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BetService betService;

    @Test
    void should_reject_stake_below_five_hundred_cop() {
        assertThatThrownBy(() -> betService.place(
                new PlaceBetRequest(MATCH_ID, "LOCAL", new BigDecimal("499.00")),
                USER,
                KEY
        ))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STAKE"));

        verify(walletRepository, never()).findWithLockByUserId(any());
    }

    @Test
    void should_reject_stake_above_five_hundred_thousand_cop() {
        assertThatThrownBy(() -> betService.place(
                new PlaceBetRequest(MATCH_ID, "LOCAL", new BigDecimal("500001.00")),
                USER,
                KEY
        ))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STAKE"));

        verify(walletRepository, never()).findWithLockByUserId(any());
    }

    @Test
    void should_reject_invalid_selection() {
        assertThatThrownBy(() -> betService.place(
                new PlaceBetRequest(MATCH_ID, "GANADOR", new BigDecimal("10000.00")),
                USER,
                KEY
        ))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_SELECTION"));

        verify(walletRepository, never()).findWithLockByUserId(any());
    }

    @Test
    void should_accept_stake_of_five_hundred_cop() {
        when(betRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(openMatch()));
        when(walletRepository.findWithLockByUserId(USER)).thenReturn(Optional.of(wallet("100000.00")));
        when(betMapper.toDto(any(), any())).thenReturn(null);

        betService.place(new PlaceBetRequest(MATCH_ID, "LOCAL", new BigDecimal("500.00")), USER, KEY);

        verify(walletRepository).save(any(WalletEntity.class));
        verify(betRepository).save(any());
        verify(notificationService).recordPending(any(), any());
    }

    @Test
    void should_report_wallet_in_cop() {
        when(walletRepository.findById(USER)).thenReturn(Optional.of(wallet("100000.00")));
        when(movementRepository.findByUserIdOrderByCreatedAtDesc(USER)).thenReturn(List.of());

        WalletDto wallet = betService.wallet(USER);

        assertThat(wallet.balance()).isEqualByComparingTo("100000.00");
        assertThat(wallet.currency()).isEqualTo("COP");
    }

    private MatchEntity openMatch() {
        MatchEntity match = new MatchEntity();
        match.setId(MATCH_ID);
        match.setSport("Fútbol");
        match.setLeague("Premier League");
        match.setHomeTeam("Liverpool");
        match.setAwayTeam("Arsenal");
        match.setHomeShort("LIV");
        match.setAwayShort("ARS");
        match.setStatus("OPEN");
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setMinute(1);
        match.setStartsAt(Instant.parse("2026-11-12T18:00:00Z"));
        match.setCreatedAt(Instant.parse("2026-11-12T18:00:00Z"));
        match.setUpdatedAt(Instant.parse("2026-11-12T18:00:00Z"));
        match.setLocalOdds(new BigDecimal("2.10"));
        match.setDrawOdds(new BigDecimal("3.40"));
        match.setAwayOdds(new BigDecimal("3.60"));
        return match;
    }

    private WalletEntity wallet(String balance) {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(USER);
        wallet.setBalance(new BigDecimal(balance));
        return wallet;
    }
}
