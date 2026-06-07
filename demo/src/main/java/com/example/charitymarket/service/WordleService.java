package com.example.charitymarket.service;

import com.example.charitymarket.dto.wordle.CreateMatchRequest;
import com.example.charitymarket.dto.wordle.GuessRequest;
import com.example.charitymarket.dto.wordle.MatchResponse;
import com.example.charitymarket.exception.BadRequestException;
import com.example.charitymarket.exception.NotFoundException;
import com.example.charitymarket.model.Charity;
import com.example.charitymarket.model.CharityDonation;
import com.example.charitymarket.model.GameType;
import com.example.charitymarket.model.User;
import com.example.charitymarket.model.WordleGuess;
import com.example.charitymarket.model.WordleMatch;
import com.example.charitymarket.model.WordleMatchPlayer;
import com.example.charitymarket.model.WordleMatchStatus;
import com.example.charitymarket.repository.CharityDonationRepository;
import com.example.charitymarket.repository.CharityRepository;
import com.example.charitymarket.repository.UserRepository;
import com.example.charitymarket.repository.WordleGuessRepository;
import com.example.charitymarket.repository.WordleMatchPlayerRepository;
import com.example.charitymarket.repository.WordleMatchRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordleService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int MAX_ATTEMPTS = 6;
    private static final List<String> WORD_BANK = List.of(
            "ALERT",
            "BLOOM",
            "LIGHT",
            "MONEY",
            "BRAVE",
            "CLEAN",
            "CLIMB",
            "CLOUD",
            "CORAL",
            "DREAM",
            "EARTH",
            "FOCUS",
            "FORGE",
            "FRESH",
            "GLOBE",
            "GRACE",
            "GRANT",
            "SHARE",
            "HEART",
            "HONOR",
            "HUMAN",
            "IDEAL",
            "LEARN",
            "LIFTY",
            "NURSE",
            "OCEAN",
            "PEACE",
            "POWER",
            "VALUE",
            "QUEST",
            "PLANT",
            "REACH",
            "RIVER",
            "ROOTS",
            "SOLAR",
            "SPARK",
            "STONE",
            "TEACH",
            "THRIVE",
            "UNITY",
            "VITAL",
            "WATER",
            "WHOLE",
            "WORLD");

    private final UserRepository userRepository;
    private final CharityRepository charityRepository;
    private final CharityDonationRepository charityDonationRepository;
    private final WordleMatchRepository wordleMatchRepository;
    private final WordleMatchPlayerRepository wordleMatchPlayerRepository;
    private final WordleGuessRepository wordleGuessRepository;

    public List<MatchResponse> getMatchesForLobby(
            Long currentUserId,
            GameType gameType,
            BigDecimal minBetAmount,
            BigDecimal maxBetAmount) {
        return wordleMatchRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(match -> match.getStatus() != WordleMatchStatus.COMPLETED)
                .filter(match -> gameType == null || match.getGameType() == gameType)
                .filter(match -> minBetAmount == null || match.getBetAmount().compareTo(minBetAmount) >= 0)
                .filter(match -> maxBetAmount == null || match.getBetAmount().compareTo(maxBetAmount) <= 0)
                .map(match -> toMatchResponse(match, currentUserId))
                .toList();
    }

    public MatchResponse getMatch(Long matchId, Long currentUserId) {
        WordleMatch match = getRequiredMatch(matchId);
        return toMatchResponse(match, currentUserId);
    }

    @Transactional
    public void cancelOpenMatch(Long currentUserId, Long matchId) {
        WordleMatch match = getRequiredMatch(matchId);
        if (match.getStatus() != WordleMatchStatus.OPEN) {
            throw new BadRequestException("Only waiting duels can be cancelled.");
        }
        if (!match.getCreator().getId().equals(currentUserId)) {
            throw new BadRequestException("Only the duel creator can cancel it.");
        }
        if (match.getOpponent() != null) {
            throw new BadRequestException("This duel already has an opponent.");
        }

        List<CharityDonation> donations = charityDonationRepository.findAllByWordleMatchId(matchId);
        for (CharityDonation donation : donations) {
            User user = donation.getUser();
            Charity charity = donation.getCharity();

            user.setBalance(normalizeMoney(user.getBalance().add(match.getBetAmount()).add(donation.getAmount())));
            charity.setTotalDonationsReceived(normalizeMoney(charity.getTotalDonationsReceived().subtract(donation.getAmount())));
            userRepository.save(user);
            charityRepository.save(charity);
        }

        charityDonationRepository.deleteAll(donations);
        wordleGuessRepository.deleteAllByMatchId(matchId);
        wordleMatchPlayerRepository.deleteAllByMatchId(matchId);
        wordleMatchRepository.delete(match);
    }

    @Transactional
    public MatchResponse createMatch(Long currentUserId, CreateMatchRequest request) {
        if (request.getGameType() != GameType.WORDLE) {
            throw new BadRequestException("That game is not live yet. Wordle is the current playable duel.");
        }

        User creator = getRequiredUser(currentUserId);
        BigDecimal betAmount = normalizeMoney(request.getBetAmount());
        BigDecimal feeAmount = calculateFee(betAmount);
        validateCanFundEntry(creator, betAmount, feeAmount);

        LocalDateTime now = LocalDateTime.now();
        WordleMatch match = wordleMatchRepository.save(WordleMatch.builder()
                .creator(creator)
                .gameType(request.getGameType())
                .solutionWord(nextSolutionWord())
                .betAmount(betAmount)
                .feeAmountPerPlayer(feeAmount)
                .prizePool(betAmount)
                .createdAt(now)
                .status(WordleMatchStatus.OPEN)
                .resultSummary("Waiting for an opponent.")
                .build());

        chargeEntry(creator, betAmount, feeAmount, match);

        wordleMatchPlayerRepository.save(WordleMatchPlayer.builder()
                .match(match)
                .user(creator)
                .attemptsUsed(0)
                .solved(false)
                .finished(false)
                .bestProgressScore(0)
                .joinedAt(now)
                .build());

        return toMatchResponse(match, currentUserId);
    }

    @Transactional
    public MatchResponse joinMatch(Long currentUserId, Long matchId) {
        User opponent = getRequiredUser(currentUserId);
        WordleMatch match = getRequiredMatch(matchId);

        if (match.getStatus() != WordleMatchStatus.OPEN) {
            throw new BadRequestException("This match is no longer open to join.");
        }
        if (match.getCreator().getId().equals(currentUserId)) {
            throw new BadRequestException("You cannot join your own match.");
        }
        if (match.getOpponent() != null) {
            throw new BadRequestException("This match already has two players.");
        }

        validateCanFundEntry(opponent, match.getBetAmount(), match.getFeeAmountPerPlayer());

        LocalDateTime now = LocalDateTime.now();
        match.setOpponent(opponent);
        match.setPrizePool(normalizeMoney(match.getBetAmount().multiply(new BigDecimal("2"))));
        match.setStatus(WordleMatchStatus.IN_PROGRESS);
        match.setStartedAt(now);
        match.setResultSummary("Match live. Both players are racing the same word.");
        wordleMatchRepository.save(match);

        chargeEntry(opponent, match.getBetAmount(), match.getFeeAmountPerPlayer(), match);

        wordleMatchPlayerRepository.save(WordleMatchPlayer.builder()
                .match(match)
                .user(opponent)
                .attemptsUsed(0)
                .solved(false)
                .finished(false)
                .bestProgressScore(0)
                .joinedAt(now)
                .build());

        return toMatchResponse(match, currentUserId);
    }

    @Transactional
    public MatchResponse submitGuess(Long currentUserId, Long matchId, GuessRequest request) {
        WordleMatch match = getRequiredMatch(matchId);
        if (match.getStatus() != WordleMatchStatus.IN_PROGRESS) {
            throw new BadRequestException("This match is not accepting guesses.");
        }

        WordleMatchPlayer player = wordleMatchPlayerRepository.findByMatchIdAndUserId(matchId, currentUserId)
                .orElseThrow(() -> new BadRequestException("You are not a participant in this match."));
        if (player.isFinished()) {
            throw new BadRequestException("Your game is already finished.");
        }

        String guess = request.getGuess().trim().toUpperCase();
        if (guess.length() != 5 || !guess.chars().allMatch(Character::isLetter)) {
            throw new BadRequestException("Guess must be exactly 5 letters.");
        }

        int guessNumber = player.getAttemptsUsed() + 1;
        String feedbackPattern = evaluateGuess(match.getSolutionWord(), guess);
        wordleGuessRepository.save(WordleGuess.builder()
                .match(match)
                .player(player)
                .guessNumber(guessNumber)
                .guessWord(guess)
                .feedbackPattern(feedbackPattern)
                .guessedAt(LocalDateTime.now())
                .build());

        player.setAttemptsUsed(guessNumber);
        player.setBestProgressScore(Math.max(player.getBestProgressScore(), progressScore(feedbackPattern)));
        if (guess.equals(match.getSolutionWord())) {
            player.setSolved(true);
            player.setFinished(true);
            player.setFinishedAt(LocalDateTime.now());
        } else if (guessNumber >= MAX_ATTEMPTS) {
            player.setFinished(true);
            player.setFinishedAt(LocalDateTime.now());
        }
        wordleMatchPlayerRepository.save(player);

        settleMatchIfReady(match);
        return toMatchResponse(match, currentUserId);
    }

    public BigDecimal getFeeRate() {
        return FEE_RATE;
    }

    private void settleMatchIfReady(WordleMatch match) {
        List<WordleMatchPlayer> players = wordleMatchPlayerRepository.findAllByMatchIdOrderByIdAsc(match.getId());
        if (players.size() < 2 || players.stream().anyMatch(player -> !player.isFinished())) {
            return;
        }

        WordleMatchPlayer first = players.get(0);
        WordleMatchPlayer second = players.get(1);
        MatchOutcome outcome = determineOutcome(match, first, second);

        match.setStatus(WordleMatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());

        if (outcome.winner().isPresent()) {
            User winner = outcome.winner().get().getUser();
            winner.setBalance(normalizeMoney(winner.getBalance().add(match.getPrizePool())));
            userRepository.save(winner);
            match.setWinner(winner);
        } else {
            refundStake(first.getUser(), match.getBetAmount());
            refundStake(second.getUser(), match.getBetAmount());
            match.setWinner(null);
        }

        match.setResultSummary(outcome.summary());
        wordleMatchRepository.save(match);
    }

    private MatchOutcome determineOutcome(WordleMatch match, WordleMatchPlayer first, WordleMatchPlayer second) {
        if (first.isSolved() && second.isSolved()) {
            int attemptsCompare = Integer.compare(first.getAttemptsUsed(), second.getAttemptsUsed());
            if (attemptsCompare < 0) {
                return winnerOutcome(match, first, "won by solving in fewer attempts.");
            }
            if (attemptsCompare > 0) {
                return winnerOutcome(match, second, "won by solving in fewer attempts.");
            }

            int timeCompare = elapsedFor(playerFinish(first), match).compareTo(elapsedFor(playerFinish(second), match));
            if (timeCompare < 0) {
                return winnerOutcome(match, first, "won on the time tiebreak.");
            }
            if (timeCompare > 0) {
                return winnerOutcome(match, second, "won on the time tiebreak.");
            }
            return splitOutcome(match, "Exact tie. Stakes refunded and fees still donated.");
        }

        if (first.isSolved()) {
            return winnerOutcome(match, first, "won by solving the puzzle.");
        }
        if (second.isSolved()) {
            return winnerOutcome(match, second, "won by solving the puzzle.");
        }

        int progressCompare = Integer.compare(first.getBestProgressScore(), second.getBestProgressScore());
        if (progressCompare > 0) {
            return winnerOutcome(match, first, "won on best board progress after six tries.");
        }
        if (progressCompare < 0) {
            return winnerOutcome(match, second, "won on best board progress after six tries.");
        }

        int timeCompare = elapsedFor(playerFinish(first), match).compareTo(elapsedFor(playerFinish(second), match));
        if (timeCompare < 0) {
            return winnerOutcome(match, first, "won the fallback time tiebreak after six tries.");
        }
        if (timeCompare > 0) {
            return winnerOutcome(match, second, "won the fallback time tiebreak after six tries.");
        }

        return splitOutcome(match, "No winner after six tries each. Stakes refunded and fees still donated.");
    }

    private MatchOutcome winnerOutcome(WordleMatch match, WordleMatchPlayer winner, String suffix) {
        String summary = winner.getUser().getName() + " " + suffix + " Prize paid: " + match.getPrizePool() + ".";
        return new MatchOutcome(Optional.of(winner), summary);
    }

    private MatchOutcome splitOutcome(WordleMatch match, String summary) {
        return new MatchOutcome(Optional.empty(), summary + " Total pool was " + match.getPrizePool() + ".");
    }

    private Duration elapsedFor(LocalDateTime finishTime, WordleMatch match) {
        return Duration.between(match.getStartedAt(), finishTime);
    }

    private LocalDateTime playerFinish(WordleMatchPlayer player) {
        return player.getFinishedAt() != null ? player.getFinishedAt() : LocalDateTime.now();
    }

    private void refundStake(User user, BigDecimal amount) {
        user.setBalance(normalizeMoney(user.getBalance().add(amount)));
        userRepository.save(user);
    }

    private void validateCanFundEntry(User user, BigDecimal betAmount, BigDecimal feeAmount) {
        if (user.getSelectedCharity() == null) {
            throw new BadRequestException("Select a charity before creating or joining a match.");
        }
        BigDecimal total = normalizeMoney(betAmount.add(feeAmount));
        if (user.getBalance().compareTo(total) < 0) {
            throw new BadRequestException("Insufficient balance for bet plus 1% fee.");
        }
    }

    private void chargeEntry(User user, BigDecimal betAmount, BigDecimal feeAmount, WordleMatch match) {
        Charity charity = user.getSelectedCharity();
        BigDecimal total = normalizeMoney(betAmount.add(feeAmount));
        user.setBalance(normalizeMoney(user.getBalance().subtract(total)));
        charity.setTotalDonationsReceived(normalizeMoney(charity.getTotalDonationsReceived().add(feeAmount)));
        userRepository.save(user);
        charityRepository.save(charity);
        charityDonationRepository.save(CharityDonation.builder()
                .user(user)
                .charity(charity)
                .wordleMatch(match)
                .amount(feeAmount)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private WordleMatch getRequiredMatch(Long matchId) {
        return wordleMatchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match not found: " + matchId));
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String nextSolutionWord() {
        long index = wordleMatchRepository.count() % WORD_BANK.size();
        return WORD_BANK.get((int) index);
    }

    private String evaluateGuess(String solutionWord, String guessWord) {
        char[] solution = solutionWord.toCharArray();
        char[] guess = guessWord.toCharArray();
        char[] result = {'B', 'B', 'B', 'B', 'B'};
        boolean[] used = new boolean[solution.length];

        for (int i = 0; i < solution.length; i++) {
            if (guess[i] == solution[i]) {
                result[i] = 'G';
                used[i] = true;
            }
        }

        for (int i = 0; i < solution.length; i++) {
            if (result[i] == 'G') {
                continue;
            }
            for (int j = 0; j < solution.length; j++) {
                if (!used[j] && guess[i] == solution[j]) {
                    result[i] = 'Y';
                    used[j] = true;
                    break;
                }
            }
        }

        return new String(result);
    }

    private int progressScore(String feedbackPattern) {
        int total = 0;
        for (char state : feedbackPattern.toCharArray()) {
            if (state == 'G') {
                total += 2;
            } else if (state == 'Y') {
                total += 1;
            }
        }
        return total;
    }

    private MatchResponse toMatchResponse(WordleMatch match, Long currentUserId) {
        List<WordleMatchPlayer> players = wordleMatchPlayerRepository.findAllByMatchIdOrderByIdAsc(match.getId());
        WordleMatchPlayer currentPlayer = players.stream()
                .filter(player -> player.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        WordleMatchPlayer opponentPlayer = players.stream()
                .filter(player -> !player.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        boolean revealOpponentGuesses = match.getStatus() == WordleMatchStatus.COMPLETED;
        return MatchResponse.builder()
                .matchId(match.getId())
                .gameType(match.getGameType().name())
                .status(match.getStatus().name())
                .betAmount(match.getBetAmount())
                .feeAmountPerPlayer(match.getFeeAmountPerPlayer())
                .prizePool(match.getPrizePool())
                .solutionWord(revealOpponentGuesses ? match.getSolutionWord() : null)
                .resultSummary(match.getResultSummary())
                .winnerName(match.getWinner() != null ? match.getWinner().getName() : null)
                .currentUserCanJoin(match.getStatus() == WordleMatchStatus.OPEN
                        && !match.getCreator().getId().equals(currentUserId))
                .currentUserCanCancel(match.getStatus() == WordleMatchStatus.OPEN
                        && match.getCreator().getId().equals(currentUserId)
                        && match.getOpponent() == null)
                .currentUserCanGuess(currentPlayer != null
                        && match.getStatus() == WordleMatchStatus.IN_PROGRESS
                        && !currentPlayer.isFinished())
                .revealOpponentGuesses(revealOpponentGuesses)
                .currentPlayer(toPlayerView(currentPlayer, true))
                .opponentPlayer(toPlayerView(opponentPlayer, revealOpponentGuesses))
                .players(players.stream()
                        .sorted(Comparator.comparing(player -> player.getUser().getId().equals(match.getCreator().getId()) ? 0 : 1))
                        .map(player -> toPlayerView(player, revealOpponentGuesses))
                        .toList())
                .build();
    }

    private MatchResponse.PlayerView toPlayerView(WordleMatchPlayer player, boolean includeGuesses) {
        if (player == null) {
            return null;
        }

        List<WordleGuess> guesses = includeGuesses
                ? wordleGuessRepository.findAllByPlayerIdOrderByGuessNumberAsc(player.getId())
                : List.of();

        List<MatchResponse.GuessView> guessViews = guesses.stream()
                .map(this::toGuessView)
                .toList();

        List<MatchResponse.GuessView> boardRows = new ArrayList<>(guessViews);
        for (int row = guessViews.size() + 1; row <= MAX_ATTEMPTS; row++) {
            boardRows.add(MatchResponse.GuessView.builder()
                    .guessNumber(row)
                    .guessWord("")
                    .feedbackPattern("")
                    .letters(List.of("", "", "", "", ""))
                    .states(List.of("empty", "empty", "empty", "empty", "empty"))
                    .build());
        }

        return MatchResponse.PlayerView.builder()
                .userId(player.getUser().getId())
                .name(player.getUser().getName())
                .joined(true)
                .solved(player.isSolved())
                .finished(player.isFinished())
                .attemptsUsed(player.getAttemptsUsed())
                .bestProgressScore(player.getBestProgressScore())
                .elapsedSeconds(player.getFinishedAt() != null && player.getMatch().getStartedAt() != null
                        ? Duration.between(player.getMatch().getStartedAt(), player.getFinishedAt()).toSeconds()
                        : null)
                .guesses(guessViews)
                .boardRows(boardRows)
                .build();
    }

    private MatchResponse.GuessView toGuessView(WordleGuess guess) {
        List<String> letters = Arrays.stream(guess.getGuessWord().split(""))
                .map(String::toUpperCase)
                .toList();
        List<String> states = guess.getFeedbackPattern().chars()
                .mapToObj(this::mapState)
                .toList();

        return MatchResponse.GuessView.builder()
                .guessNumber(guess.getGuessNumber())
                .guessWord(guess.getGuessWord())
                .feedbackPattern(guess.getFeedbackPattern())
                .letters(letters)
                .states(states)
                .build();
    }

    private String mapState(int codePoint) {
        return switch ((char) codePoint) {
            case 'G' -> "correct";
            case 'Y' -> "present";
            default -> "absent";
        };
    }

    private record MatchOutcome(Optional<WordleMatchPlayer> winner, String summary) {
    }
}
