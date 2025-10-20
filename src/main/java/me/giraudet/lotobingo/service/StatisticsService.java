package me.giraudet.lotobingo.service;

import lombok.RequiredArgsConstructor;
import me.giraudet.lotobingo.entity.BingoCardEntity;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for calculating game statistics and probabilities.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final GameRepository gameRepository;
    private final BingoCardService bingoCardService;
    private final DrawService drawService;

    /**
     * Calculate game statistics.
     */
    @Transactional(readOnly = true)
    public GameStatistics calculateStatistics(UUID gameId, Integer drawCount) {
        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameService.GameNotFoundException(gameId));

        List<BingoCardEntity> cards = bingoCardService.getCardsByGame(gameId);
        int totalCards = cards.size();

        // If no draw count specified, use current draw count
        if (drawCount == null) {
            drawCount = (int) drawService.getDrawnNumbers(gameId).size();
        }

        // Calculate probabilities
        double probabilityFirstWin = calculateProbabilityFirstWin(totalCards, game.getWordsPerCard(), drawCount);
        double expectedWinners = calculateExpectedWinners(totalCards, game.getWordsPerCard(), drawCount);

        // Calculate probability by draw
        List<DrawProbability> probabilityByDraw = calculateProbabilityByDraw(
                totalCards,
                game.getWordsPerCard(),
                Math.min(drawCount, game.getWords().size())
        );

        return new GameStatistics(
                totalCards,
                drawCount,
                probabilityFirstWin,
                expectedWinners,
                probabilityByDraw
        );
    }

    /**
     * Calculate probability of having at least one winner.
     * Simplified calculation: P(at least one win) = 1 - P(no wins)^totalCards
     */
    private double calculateProbabilityFirstWin(int totalCards, int wordsPerCard, int drawCount) {
        // Probability that a single card wins
        double pSingleCardWin = calculateSingleCardWinProbability(wordsPerCard, drawCount);

        // Probability that at least one card wins
        return 1.0 - Math.pow(1.0 - pSingleCardWin, totalCards);
    }

    /**
     * Calculate expected number of winners.
     */
    private double calculateExpectedWinners(int totalCards, int wordsPerCard, int drawCount) {
        double pSingleCardWin = calculateSingleCardWinProbability(wordsPerCard, drawCount);
        return totalCards * pSingleCardWin;
    }

    /**
     * Simplified probability that a single card wins with given draws.
     * Uses hypergeometric distribution approximation.
     */
    private double calculateSingleCardWinProbability(int wordsPerCard, int drawCount) {
        // Simplified: if we draw more numbers than words on card, probability approaches 1
        if (drawCount >= wordsPerCard) {
            // More precise: probability all words are drawn
            // For simplicity, using linear approximation
            return Math.min(1.0, (double) drawCount / (wordsPerCard * 2));
        }

        // If fewer draws than words needed, lower probability
        return Math.pow((double) drawCount / wordsPerCard, 2) * 0.5;
    }

    /**
     * Calculate cumulative probability for each draw number.
     */
    private List<DrawProbability> calculateProbabilityByDraw(int totalCards, int wordsPerCard, int maxDraws) {
        List<DrawProbability> probabilities = new ArrayList<>();

        for (int draw = 1; draw <= maxDraws; draw++) {
            double cumulative = calculateProbabilityFirstWin(totalCards, wordsPerCard, draw);
            probabilities.add(new DrawProbability(draw, cumulative));
        }

        return probabilities;
    }

    // Inner classes
    public static class GameStatistics {
        public final int totalCards;
        public final int drawCount;
        public final double probabilityFirstWin;
        public final double expectedWinners;
        public final List<DrawProbability> probabilityByDraw;

        public GameStatistics(int totalCards, int drawCount, double probabilityFirstWin,
                            double expectedWinners, List<DrawProbability> probabilityByDraw) {
            this.totalCards = totalCards;
            this.drawCount = drawCount;
            this.probabilityFirstWin = probabilityFirstWin;
            this.expectedWinners = expectedWinners;
            this.probabilityByDraw = probabilityByDraw;
        }
    }

    public static class DrawProbability {
        public final int drawNumber;
        public final double cumulativeProbability;

        public DrawProbability(int drawNumber, double cumulativeProbability) {
            this.drawNumber = drawNumber;
            this.cumulativeProbability = cumulativeProbability;
        }
    }
}
