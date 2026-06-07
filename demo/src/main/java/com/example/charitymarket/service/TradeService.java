package com.example.charitymarket.service;

import com.example.charitymarket.dto.TradeRequest;
import com.example.charitymarket.dto.TradeResponse;
import com.example.charitymarket.exception.BadRequestException;
import com.example.charitymarket.exception.NotFoundException;
import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.CharityDonation;
import com.example.charitymarket.model.Market;
import com.example.charitymarket.model.MarketStatus;
import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.Position;
import com.example.charitymarket.model.Trade;
import com.example.charitymarket.model.TradeSide;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityDonationRepository;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.MarketRepository;
import com.example.charitymarket.repository.PositionRepository;
import com.example.charitymarket.repository.TradeRepository;
import com.example.charitymarket.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal BUY_GROSS_DIVISOR = BigDecimal.ONE.add(FEE_RATE);
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final CharityDonationRepository charityDonationRepository;
    private final CharityRepository charityRepository;

    @Transactional
    public TradeResponse executeTrade(TradeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Market market = marketRepository.findById(request.getMarketId())
                .orElseThrow(() -> new NotFoundException("Market not found"));

        if (market.getStatus() != MarketStatus.OPEN) {
            throw new BadRequestException("Market is not open");
        }

        Charity charity = user.getSelectedCharity();
        if (charity == null) {
            throw new BadRequestException("User has no selected charity");
        }

        BigDecimal price = getCurrentPrice(market, request.getOutcome());
        TradeInputs tradeInputs = resolveTradeInputs(request, price);
        BigDecimal quantity = tradeInputs.quantity();
        BigDecimal tradeValue = tradeInputs.tradeValue();
        BigDecimal fee = tradeInputs.fee();

        Position position = positionRepository
                .findByUserIdAndMarketIdAndOutcome(user.getId(), market.getId(), request.getOutcome())
                .orElse(null);

        if (request.getSide() == TradeSide.BUY) {
            handleBuy(user, position, market, request.getOutcome(), quantity, price, tradeValue, fee);
        } else {
            position = handleSell(user, position, quantity, price, tradeValue, fee);
        }

        Trade trade = tradeRepository.save(Trade.builder()
                .user(user)
                .market(market)
                .outcome(request.getOutcome())
                .side(request.getSide())
                .quantity(quantity)
                .price(price)
                .tradeValue(tradeValue)
                .fee(fee)
                .charity(charity)
                .createdAt(LocalDateTime.now())
                .build());

        charityDonationRepository.save(CharityDonation.builder()
                .user(user)
                .charity(charity)
                .trade(trade)
                .amount(fee)
                .createdAt(trade.getCreatedAt())
                .build());

        charity.setTotalDonationsReceived(normalizeMoney(charity.getTotalDonationsReceived().add(fee)));
        charityRepository.save(charity);
        userRepository.save(user);

        return toTradeResponse(trade);
    }

    private void handleBuy(
            User user,
            Position existingPosition,
            Market market,
            Outcome outcome,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal tradeValue,
            BigDecimal fee) {
        BigDecimal totalCost = normalizeMoney(tradeValue.add(fee));
        if (user.getBalance().compareTo(totalCost) < 0) {
            throw new BadRequestException("Insufficient balance");
        }

        user.setBalance(normalizeMoney(user.getBalance().subtract(totalCost)));

        if (existingPosition == null) {
            positionRepository.save(Position.builder()
                    .user(user)
                    .market(market)
                    .outcome(outcome)
                    .quantity(quantity)
                    .averageEntryPrice(price)
                    .realizedPnl(ZERO)
                    .build());
            return;
        }

        BigDecimal oldQuantity = existingPosition.getQuantity();
        BigDecimal newQuantity = normalizeMoney(oldQuantity.add(quantity));
        BigDecimal weightedCost = oldQuantity.multiply(existingPosition.getAverageEntryPrice())
                .add(quantity.multiply(price));
        BigDecimal newAverageEntryPrice = weightedCost
                .divide(newQuantity, 2, RoundingMode.HALF_UP);

        existingPosition.setQuantity(newQuantity);
        existingPosition.setAverageEntryPrice(newAverageEntryPrice);
        positionRepository.save(existingPosition);
    }

    private Position handleSell(
            User user,
            Position existingPosition,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal tradeValue,
            BigDecimal fee) {
        if (existingPosition == null || existingPosition.getQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient position quantity");
        }

        user.setBalance(normalizeMoney(user.getBalance().add(tradeValue).subtract(fee)));

        BigDecimal realizedPnlForSale = quantity
                .multiply(price.subtract(existingPosition.getAverageEntryPrice()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal newQuantity = normalizeMoney(existingPosition.getQuantity().subtract(quantity));

        existingPosition.setQuantity(newQuantity);
        existingPosition.setRealizedPnl(normalizeMoney(existingPosition.getRealizedPnl().add(realizedPnlForSale)));
        positionRepository.save(existingPosition);
        return existingPosition;
    }

    private BigDecimal getCurrentPrice(Market market, Outcome outcome) {
        return outcome == Outcome.YES ? market.getYesPrice() : market.getNoPrice();
    }

    private TradeInputs resolveTradeInputs(TradeRequest request, BigDecimal price) {
        if (request.getSide() == TradeSide.BUY && request.getTotalAmount() != null) {
            BigDecimal totalAmount = normalizeMoney(request.getTotalAmount());
            if (totalAmount.compareTo(ZERO) <= 0) {
                throw new BadRequestException("Total spend is invalid");
            }

            BigDecimal budgetBeforeFee = totalAmount.divide(BUY_GROSS_DIVISOR, 2, RoundingMode.DOWN);
            BigDecimal quantity = budgetBeforeFee.divide(price, 2, RoundingMode.DOWN);
            if (quantity.compareTo(ZERO) <= 0) {
                throw new BadRequestException("Total spend is too low for this market price");
            }

            BigDecimal tradeValue = calculateTradeValue(quantity, price);
            BigDecimal fee = calculateFee(tradeValue);
            return new TradeInputs(quantity, tradeValue, fee);
        }

        if (request.getQuantity() == null) {
            throw new BadRequestException(request.getSide() == TradeSide.BUY
                    ? "Enter a total spend or quantity"
                    : "Quantity is required");
        }

        BigDecimal quantity = normalizeMoney(request.getQuantity());
        if (quantity.compareTo(ZERO) <= 0) {
            throw new BadRequestException("Quantity is invalid");
        }

        BigDecimal tradeValue = calculateTradeValue(quantity, price);
        BigDecimal fee = calculateFee(tradeValue);
        return new TradeInputs(quantity, tradeValue, fee);
    }

    private BigDecimal calculateFee(BigDecimal tradeValue) {
        return tradeValue.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTradeValue(BigDecimal quantity, BigDecimal price) {
        return quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getFeeRate() {
        return FEE_RATE;
    }

    public TradeResponse toTradeResponse(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .userId(trade.getUser().getId())
                .userName(trade.getUser().getName())
                .marketId(trade.getMarket().getId())
                .marketQuestion(trade.getMarket().getQuestion())
                .outcome(trade.getOutcome())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .tradeValue(trade.getTradeValue())
                .fee(trade.getFee())
                .charityName(trade.getCharity().getName())
                .createdAt(trade.getCreatedAt())
                .build();
    }

    private record TradeInputs(BigDecimal quantity, BigDecimal tradeValue, BigDecimal fee) {
    }
}
