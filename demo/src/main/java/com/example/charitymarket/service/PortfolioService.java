package com.example.charitymarket.service;

import com.example.charitymarket.dto.DonationResponse;
import com.example.charitymarket.dto.PortfolioResponse;
import com.example.charitymarket.dto.PositionResponse;
import com.example.charitymarket.exception.NotFoundException;
import com.example.charitymarket.model.CharityDonation;
import com.example.charitymarket.model.Outcome;
import com.example.charitymarket.model.Position;
import com.example.charitymarket.model.User;
import com.example.charitymarket.repository.CharityDonationRepository;
import com.example.charitymarket.repository.PositionRepository;
import com.example.charitymarket.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final CharityDonationRepository charityDonationRepository;

    public PortfolioResponse getPortfolio(Long userId) {
        User user = getUser(userId);
        List<Position> allPositions = positionRepository.findByUserId(userId);
        List<CharityDonation> donations = charityDonationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        BigDecimal totalRealizedPnl = allPositions.stream()
                .map(Position::getRealizedPnl)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUnrealizedPnl = allPositions.stream()
                .map(this::calculateUnrealizedPnl)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDonated = donations.stream()
                .map(CharityDonation::getAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return PortfolioResponse.builder()
                .userId(user.getId())
                .userName(user.getName())
                .balance(user.getBalance())
                .selectedCharityName(user.getSelectedCharity() != null ? user.getSelectedCharity().getName() : null)
                .totalRealizedPnl(totalRealizedPnl)
                .totalUnrealizedPnl(totalUnrealizedPnl)
                .totalPnl(totalRealizedPnl.add(totalUnrealizedPnl).setScale(2, RoundingMode.HALF_UP))
                .totalDonated(totalDonated)
                .positions(allPositions.stream()
                        .filter(position -> position.getQuantity().compareTo(ZERO) > 0)
                        .map(this::toPositionResponse)
                        .toList())
                .build();
    }

    public List<PositionResponse> getPositions(Long userId) {
        getUser(userId);
        return positionRepository.findByUserId(userId).stream()
                .filter(position -> position.getQuantity().compareTo(ZERO) > 0)
                .map(this::toPositionResponse)
                .toList();
    }

    public List<DonationResponse> getDonations(Long userId) {
        getUser(userId);
        return charityDonationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDonationResponse)
                .toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private PositionResponse toPositionResponse(Position position) {
        BigDecimal currentPrice = getCurrentPrice(position);
        BigDecimal unrealizedPnl = calculateUnrealizedPnl(position);
        BigDecimal totalPnl = position.getRealizedPnl().add(unrealizedPnl).setScale(2, RoundingMode.HALF_UP);

        return PositionResponse.builder()
                .id(position.getId())
                .marketId(position.getMarket().getId())
                .marketQuestion(position.getMarket().getQuestion())
                .outcome(position.getOutcome())
                .quantity(position.getQuantity())
                .averageEntryPrice(position.getAverageEntryPrice())
                .currentPrice(currentPrice)
                .realizedPnl(position.getRealizedPnl())
                .unrealizedPnl(unrealizedPnl)
                .totalPnl(totalPnl)
                .build();
    }

    private DonationResponse toDonationResponse(CharityDonation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .charityName(donation.getCharity().getName())
                .amount(donation.getAmount())
                .tradeId(donation.getTrade().getId())
                .createdAt(donation.getCreatedAt())
                .build();
    }

    private BigDecimal calculateUnrealizedPnl(Position position) {
        return position.getQuantity()
                .multiply(getCurrentPrice(position).subtract(position.getAverageEntryPrice()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCurrentPrice(Position position) {
        return position.getOutcome() == Outcome.YES
                ? position.getMarket().getYesPrice()
                : position.getMarket().getNoPrice();
    }
}
