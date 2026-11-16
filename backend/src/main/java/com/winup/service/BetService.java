package com.winup.service;

import com.winup.entity.BetEntity;
import com.winup.entity.MatchEntity;
import com.winup.model.BetStatus;
import com.winup.model.MatchStatus;
import com.winup.model.SportCatalog;
import com.winup.model.vo.BetSelection;
import com.winup.model.vo.IdempotencyKey;
import com.winup.model.vo.Stake;
import com.winup.exception.ValidationException;
import com.winup.entity.MovementEntity;
import com.winup.entity.WalletEntity;
import com.winup.dto.BetDto;
import com.winup.dto.DtoMapper;
import com.winup.mapper.BetMapper;
import com.winup.dto.MovementDto;
import com.winup.dto.PlaceBetRequest;
import com.winup.dto.WalletDto;
import com.winup.repository.BetRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.MovementRepository;
import com.winup.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BetService {
    private final MatchRepository matchRepository;
    private final BetRepository betRepository;
    private final WalletRepository walletRepository;
    private final MovementRepository movementRepository;
    private final EventPublisher eventPublisher;
    private final BetMapper betMapper;
    private final NotificationService notificationService;

    public BetService(
            MatchRepository matchRepository,
            BetRepository betRepository,
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            EventPublisher eventPublisher,
            BetMapper betMapper,
            NotificationService notificationService
    ) {
        this.matchRepository = matchRepository;
        this.betRepository = betRepository;
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
        this.betMapper = betMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public BetDto place(PlaceBetRequest request, String userId, String idempotencyKey) {
        if (request == null || request.matchId() == null || request.stake() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de apuesta incompletos");
        }
        IdempotencyKey key = new IdempotencyKey(idempotencyKey);

        BetEntity existing = betRepository.findByIdempotencyKey(key.value()).orElse(null);
        if (existing != null) {
            MatchEntity match = matchRepository.findById(existing.getMatchId()).orElse(null);
            return betMapper.toDto(existing, match);
        }

        BetSelection selection = BetSelection.from(request.selection());
        Stake stake = new Stake(request.stake());

        MatchEntity match = matchRepository.findById(request.matchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido no encontrado"));

        if (MatchStatus.from(match.getStatus()) != MatchStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El partido ya no admite apuestas");
        }

        if (selection == BetSelection.EMPATE && !SportCatalog.hasDraw(match.getSport())) {
            throw new ValidationException("INVALID_SELECTION", "Este deporte no admite empate");
        }

        WalletEntity wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartera no encontrada"));

        if (wallet.getBalance().compareTo(stake.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente");
        }

        BigDecimal odds = switch (selection) {
            case LOCAL -> match.getLocalOdds();
            case EMPATE -> match.getDrawOdds();
            case VISITANTE -> match.getAwayOdds();
        };

        wallet.setBalance(wallet.getBalance().subtract(stake.amount()).setScale(2, RoundingMode.HALF_EVEN));
        walletRepository.save(wallet);

        MovementEntity movement = new MovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setUserId(userId);
        movement.setConcept("Apuesta " + match.getHomeTeam() + " vs " + match.getAwayTeam());
        movement.setAmount(stake.amount().negate());
        movement.setType("GASTO");
        movement.setCreatedAt(Instant.now());
        movementRepository.save(movement);

        BetEntity bet = new BetEntity();
        bet.setId(UUID.randomUUID());
        bet.setMatchId(match.getId());
        bet.setUserId(userId);
        bet.setSelection(selection.name());
        bet.setStake(stake.amount());
        bet.setOdds(odds);
        bet.setStatus(BetStatus.PENDING.code());
        bet.setPayout(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        bet.setIdempotencyKey(key.value());
        bet.setCreatedAt(Instant.now());
        betRepository.save(bet);
        notificationService.recordPending(bet, match);

        eventPublisher.betPlaced(bet, match);
        return betMapper.toDto(bet, match);
    }

    public List<BetDto> list(String userId) {
        return betRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(bet -> betMapper.toDto(bet, matchRepository.findById(bet.getMatchId()).orElse(null)))
                .toList();
    }

    public WalletDto wallet(String userId) {
        WalletEntity wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartera no encontrada"));
        List<MovementDto> movements = movementRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DtoMapper::toMovement)
                .toList();
        return new WalletDto(wallet.getBalance(), wallet.getHeldBalance(), "COP", movements);
    }
}
