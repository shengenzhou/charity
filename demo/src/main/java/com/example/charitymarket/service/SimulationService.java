package com.example.charitymarket.service;

import com.example.charitymarket.config.AuthMode;
import com.example.charitymarket.config.HackathonAuthProperties;
import com.example.charitymarket.dto.SimulationState;
import com.example.charitymarket.exception.BadRequestException;
import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketPriceSnapshot;
import com.example.charitymarket.model.MarketStatus;
import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.Position;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityDonationRepository;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.MarketPriceSnapshotRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.PositionRepository;
import com.example.charitymarket.repository.SimulationStateRepository;
import com.example.charitymarket.repository.TradeRepository;
import com.example.charitymarket.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final Long STATE_ID = 1L;
    private static final int MIN_TIMESTAMP = 0;
    private static final int MAX_TIMESTAMP = 4;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal ONE = new BigDecimal("1.00");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.05");
    private static final BigDecimal MAX_PRICE = new BigDecimal("0.95");
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000.00");
    private static final Map<String, BigDecimal> DEFAULT_CHARITY_TOTALS = Map.of(
            "Red Cross", new BigDecimal("125000.00"),
            "WWF", new BigDecimal("98000.00"),
            "Doctors Without Borders", new BigDecimal("143500.00"));
    private static final Map<String, String> DEFAULT_USER_CHARITIES = Map.of(
            "Alice", "Red Cross",
            "Bob", "WWF",
            "Charlie", "Doctors Without Borders");
    private static final Map<String, String> DEFAULT_USER_EMAILS = Map.of(
            "Alice", "alice@example.com",
            "Bob", "bob@example.com",
            "Charlie", "charlie@example.com");

    private final MarketRepository marketRepository;
    private final MarketPriceSnapshotRepository marketPriceSnapshotRepository;
    private final SimulationStateRepository simulationStateRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final CharityDonationRepository charityDonationRepository;
    private final UserRepository userRepository;
    private final CharityRepository charityRepository;
    private final HackathonAuthProperties authProperties;

    public void initializeSimulation() {
        getOrCreateSimulationStateEntity();

        List<Market> markets = marketRepository.findAll().stream()
                .sorted(Comparator.comparing(Market::getId))
                .toList();

        boolean missingSnapshots = markets.stream()
                .anyMatch(market -> marketPriceSnapshotRepository.findByMarketIdOrderByTimestampIndexAsc(market.getId()).size() != 5);

        if (missingSnapshots) {
            generateSnapshotsForAllMarkets();
        }
    }

    public Integer getCurrentGlobalTimestamp() {
        return getOrCreateSimulationStateEntity().getCurrentTimestamp();
    }

    public SimulationState getSimulationState() {
        com.example.charitymarket.model.SimulationState state = getOrCreateSimulationStateEntity();
        return SimulationState.builder()
                .currentTimestamp(state.getCurrentTimestamp())
                .expiryTimestamp(MAX_TIMESTAMP)
                .resolved(state.getCurrentTimestamp() >= MAX_TIMESTAMP)
                .build();
    }

    public void generateSnapshotsForAllMarkets() {
        marketRepository.findAll().forEach(this::generateSnapshotsForMarket);
    }

    public void generateSnapshotsForMarket(Market market) {
        marketPriceSnapshotRepository.deleteAllByMarket(market);

        BigDecimal initialYes = normalizePrice(market.getInitialYesPrice() != null ? market.getInitialYesPrice() : market.getYesPrice());
        long seed = market.getId() != null
                ? market.getId() * 9973L
                : Math.abs(market.getQuestion().hashCode()) * 31L;
        Random random = new Random(seed);
        Outcome finalOutcome = random.nextBoolean() ? Outcome.YES : Outcome.NO;
        BigDecimal targetYes = finalOutcome == Outcome.YES ? ONE : ZERO;

        saveSnapshot(market, 0, initialYes);

        BigDecimal currentYes = initialYes;
        for (int timestamp = 1; timestamp <= 3; timestamp++) {
            int remainingSteps = MAX_TIMESTAMP - timestamp + 1;
            BigDecimal drift = targetYes.subtract(currentYes)
                    .divide(BigDecimal.valueOf(remainingSteps), 4, RoundingMode.HALF_UP);
            BigDecimal noise = BigDecimal.valueOf((random.nextDouble() * 0.16d) - 0.08d)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal nextYes = clamp(currentYes.add(drift).add(noise));
            saveSnapshot(market, timestamp, nextYes);
            currentYes = nextYes;
        }

        saveSnapshot(market, MAX_TIMESTAMP, targetYes);
    }

    @Transactional
    public void setGlobalTimestamp(Integer timestampIndex) {
        validateTimestamp(timestampIndex);

        com.example.charitymarket.model.SimulationState state = getOrCreateSimulationStateEntity();
        if (timestampIndex < state.getCurrentTimestamp()) {
            throw new BadRequestException("Cannot move simulation backwards. Reset first.");
        }

        for (Market market : marketRepository.findAll()) {
            MarketPriceSnapshot snapshot = marketPriceSnapshotRepository
                    .findByMarketIdAndTimestampIndex(market.getId(), timestampIndex)
                    .orElseThrow(() -> new BadRequestException("Missing snapshot for market " + market.getId()
                            + " at timestamp " + timestampIndex));

            market.setYesPrice(snapshot.getYesPrice());
            market.setNoPrice(snapshot.getNoPrice());
            market.setCurrentTimestamp(timestampIndex);

            if (timestampIndex == MAX_TIMESTAMP) {
                resolveAndPayoutMarket(market);
            } else if (market.getStatus() != MarketStatus.RESOLVED) {
                market.setStatus(MarketStatus.OPEN);
            }

            marketRepository.save(market);
        }

        state.setCurrentTimestamp(timestampIndex);
        simulationStateRepository.save(state);
    }

    @Transactional
    public void resolveAndPayoutMarket(Market market) {
        if (market.isPayoutCompleted()) {
            market.setStatus(MarketStatus.RESOLVED);
            return;
        }

        Outcome resolvedOutcome = ONE.compareTo(normalizePrice(market.getYesPrice())) == 0 ? Outcome.YES : Outcome.NO;
        List<Position> positions = positionRepository.findByMarketId(market.getId()).stream()
                .filter(position -> position.getQuantity().compareTo(ZERO) > 0)
                .toList();

        for (Position position : positions) {
            BigDecimal payout = position.getOutcome() == resolvedOutcome ? position.getQuantity() : ZERO;
            BigDecimal costBasis = position.getQuantity()
                    .multiply(position.getAverageEntryPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalPnl = payout.subtract(costBasis).setScale(2, RoundingMode.HALF_UP);

            User user = position.getUser();
            user.setBalance(normalizeMoney(user.getBalance().add(payout)));
            userRepository.save(user);

            position.setRealizedPnl(normalizeMoney(position.getRealizedPnl().add(finalPnl)));
            position.setQuantity(ZERO);
            positionRepository.save(position);
        }

        market.setResolvedOutcome(resolvedOutcome);
        market.setStatus(MarketStatus.RESOLVED);
        market.setPayoutCompleted(true);
        market.setCurrentTimestamp(MAX_TIMESTAMP);
    }

    @Transactional
    public void resetSimulation() {
        charityDonationRepository.deleteAllInBatch();
        tradeRepository.deleteAllInBatch();
        positionRepository.deleteAllInBatch();

        List<Charity> charities = charityRepository.findAll();
        for (Charity charity : charities) {
            charity.setTotalDonationsReceived(DEFAULT_CHARITY_TOTALS.getOrDefault(charity.getName(), ZERO));
        }
        charityRepository.saveAll(charities);

        Map<String, Charity> charityByName = charities.stream()
                .collect(java.util.stream.Collectors.toMap(Charity::getName, charity -> charity));

        if (authProperties.getMode() == AuthMode.DEMO) {
            ensureDefaultDemoUsers(charityByName);
        }

        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.setBalance(DEFAULT_BALANCE);
            String defaultCharityName = DEFAULT_USER_CHARITIES.get(user.getName());
            if (defaultCharityName != null) {
                user.setSelectedCharity(charityByName.get(defaultCharityName));
            }
        }
        userRepository.saveAll(users);

        List<Market> markets = marketRepository.findAll().stream()
                .sorted(Comparator.comparing(Market::getId))
                .toList();

        for (int i = 0; i < markets.size(); i++) {
            Market market = markets.get(i);
            market.setYesPrice(normalizePrice(market.getInitialYesPrice()));
            market.setNoPrice(normalizePrice(market.getInitialNoPrice()));
            market.setCurrentTimestamp(MIN_TIMESTAMP);
            market.setExpiryTimestamp(MAX_TIMESTAMP);
            market.setExpiresAt(LocalDateTime.now().plusDays(i + 1L));
            market.setResolvedOutcome(null);
            market.setPayoutCompleted(false);
            market.setStatus(MarketStatus.OPEN);
            marketRepository.save(market);
        }

        marketPriceSnapshotRepository.deleteAllInBatch();
        generateSnapshotsForAllMarkets();

        com.example.charitymarket.model.SimulationState state = getOrCreateSimulationStateEntity();
        state.setCurrentTimestamp(MIN_TIMESTAMP);
        simulationStateRepository.save(state);
    }

    private void ensureDefaultDemoUsers(Map<String, Charity> charityByName) {
        List<User> existingUsers = userRepository.findAll();
        Map<String, User> usersByName = existingUsers.stream()
                .filter(user -> user.getName() != null)
                .collect(java.util.stream.Collectors.toMap(User::getName, user -> user, (left, right) -> left));

        for (Map.Entry<String, String> entry : DEFAULT_USER_CHARITIES.entrySet()) {
            String userName = entry.getKey();
            if (usersByName.containsKey(userName)) {
                continue;
            }

            userRepository.save(User.builder()
                    .name(userName)
                    .email(DEFAULT_USER_EMAILS.get(userName))
                    .balance(DEFAULT_BALANCE)
                    .usernameConfigured(true)
                    .selectedCharity(charityByName.get(entry.getValue()))
                    .build());
        }
    }

    private void validateTimestamp(Integer timestampIndex) {
        if (timestampIndex == null || timestampIndex < MIN_TIMESTAMP || timestampIndex > MAX_TIMESTAMP) {
            throw new BadRequestException("Timestamp must be between 0 and 4.");
        }
    }

    private com.example.charitymarket.model.SimulationState getOrCreateSimulationStateEntity() {
        return simulationStateRepository.findById(STATE_ID)
                .orElseGet(() -> simulationStateRepository.save(com.example.charitymarket.model.SimulationState.builder()
                        .id(STATE_ID)
                        .currentTimestamp(MIN_TIMESTAMP)
                        .build()));
    }

    private void saveSnapshot(Market market, int timestampIndex, BigDecimal yesPrice) {
        BigDecimal normalizedYes = normalizePrice(yesPrice);
        BigDecimal noPrice = ONE.subtract(normalizedYes).setScale(2, RoundingMode.HALF_UP);

        marketPriceSnapshotRepository.save(MarketPriceSnapshot.builder()
                .market(market)
                .timestampIndex(timestampIndex)
                .yesPrice(normalizedYes)
                .noPrice(noPrice)
                .simulatedAt(LocalDateTime.now().plusHours(timestampIndex))
                .build());
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(MIN_PRICE) < 0) {
            return MIN_PRICE;
        }
        if (value.compareTo(MAX_PRICE) > 0) {
            return MAX_PRICE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
