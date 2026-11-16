package com.winup.service;

import com.winup.entity.PaymentIntentEntity;
import com.winup.entity.UserEntity;
import com.winup.model.MatchStatus;
import com.winup.model.vo.PromoDetail;
import com.winup.model.vo.PromoTitle;
import com.winup.dto.AdminDashboardDto;
import com.winup.dto.AdminLedgerDto;
import com.winup.dto.AdminPaymentDto;
import com.winup.dto.AdminUserDto;
import com.winup.dto.MatchDto;
import com.winup.dto.MatchPageDto;
import com.winup.mapper.MatchMapper;
import com.winup.service.provider.random.RandomAdapter;
import com.winup.repository.BetRepository;
import com.winup.repository.LedgerEntryRepository;
import com.winup.repository.MatchRepository;
import com.winup.repository.PaymentIntentRepository;
import com.winup.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class AdminService {
    private static final Set<String> ROLES = Set.of("USER", "ADMIN");

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final BetRepository betRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final GeneratorService generatorService;
    private final RandomAdapter randomAdapter;
    private final MatchMapper matchMapper;
    private final EventPublisher eventPublisher;

    public AdminService(
            UserRepository userRepository,
            MatchRepository matchRepository,
            MatchService matchService,
            BetRepository betRepository,
            PaymentIntentRepository paymentIntentRepository,
            LedgerEntryRepository ledgerEntryRepository,
            GeneratorService generatorService,
            RandomAdapter randomAdapter,
            MatchMapper matchMapper,
            EventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.betRepository = betRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.generatorService = generatorService;
        this.randomAdapter = randomAdapter;
        this.matchMapper = matchMapper;
        this.eventPublisher = eventPublisher;
    }

    public AdminDashboardDto dashboard() {
        BigDecimal debits = ledgerEntryRepository.sumDebits();
        BigDecimal credits = ledgerEntryRepository.sumCredits();
        return new AdminDashboardDto(
                userRepository.count(),
                matchRepository.count(),
                betRepository.count(),
                paymentIntentRepository.count(),
                debits == null ? "0.00" : debits.toPlainString(),
                credits == null ? "0.00" : credits.toPlainString()
        );
    }

    public void promote(String title, String detail) {
        eventPublisher.promotion(
                new PromoTitle(title).value(),
                new PromoDetail(detail).value()
        );
    }

    public List<AdminUserDto> users() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(user -> new AdminUserDto(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.getStatus(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public AdminUserDto updateRole(String userId, String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Rol inválido");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setRole(normalized);
        userRepository.save(user);
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    public List<AdminPaymentDto> payments() {
        return paymentIntentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toPayment)
                .toList();
    }

    public List<AdminLedgerDto> ledger() {
        return ledgerEntryRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(entry -> new AdminLedgerDto(
                        entry.getId(),
                        entry.getTransactionId(),
                        entry.getAccount(),
                        entry.getDebit(),
                        entry.getCredit(),
                        entry.getCurrency(),
                        entry.getConcept(),
                        entry.getCreatedAt()
                ))
                .toList();
    }

    public List<MatchDto> matches() {
        return matchRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(50)
                .map(matchMapper::toDto)
                .toList();
    }

    public MatchPageDto matchesPage(int page, int size, String status, String sport) {
        return matchService.page(status, sport, page, size);
    }

    public int ensureSports() {
        generatorService.ensureAllSports();
        return matchRepository.findDistinctSportsByStatus(MatchStatus.OPEN.code()).size();
    }

    public int seed(int count) {
        return randomAdapter.seedExtra(count);
    }

    private AdminPaymentDto toPayment(PaymentIntentEntity intent) {
        return new AdminPaymentDto(
                intent.getId(),
                intent.getUserId(),
                intent.getStatus(),
                intent.getAmountCop(),
                intent.getProvider(),
                intent.getCreatedAt()
        );
    }
}
