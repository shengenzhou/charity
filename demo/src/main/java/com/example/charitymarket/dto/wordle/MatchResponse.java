package com.example.charitymarket.dto.wordle;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {

    private Long matchId;
    private String gameType;
    private String status;
    private BigDecimal betAmount;
    private BigDecimal feeAmountPerPlayer;
    private BigDecimal prizePool;
    private String solutionWord;
    private String resultSummary;
    private String winnerName;
    private boolean currentUserCanJoin;
    private boolean currentUserCanCancel;
    private boolean currentUserCanGuess;
    private boolean revealOpponentGuesses;
    private PlayerView currentPlayer;
    private PlayerView opponentPlayer;
    private List<PlayerView> players;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerView {
        private Long userId;
        private String name;
        private boolean joined;
        private boolean solved;
        private boolean finished;
        private int attemptsUsed;
        private Integer bestProgressScore;
        private Long elapsedSeconds;
        private List<GuessView> guesses;
        private List<GuessView> boardRows;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuessView {
        private int guessNumber;
        private String guessWord;
        private String feedbackPattern;
        private List<String> letters;
        private List<String> states;
    }
}
