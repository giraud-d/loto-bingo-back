package me.giraudet.lotobingo.service;

import lombok.RequiredArgsConstructor;
import me.giraudet.lotobingo.entity.DrawEntity;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import me.giraudet.lotobingo.repository.DrawRepository;
import me.giraudet.lotobingo.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing game draws.
 */
@Service
@RequiredArgsConstructor
public class DrawService {

    private final DrawRepository drawRepository;
    private final GameRepository gameRepository;

    /**
     * Draw a number/word in a game.
     */
    @Transactional
    public DrawEntity drawNumber(UUID gameId, Integer number) {
        // Validate game exists
        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameService.GameNotFoundException(gameId));

        // Validate number is not already drawn
        if (drawRepository.existsByGameIdAndNumber(gameId, number)) {
            throw new NumberAlreadyDrawnException(number);
        }

        // Find the word associated with this number
        WordMappingEntity wordMapping = game.getWords().stream()
                .filter(w -> w.getNumber().equals(number))
                .findFirst()
                .orElseThrow(() -> new InvalidNumberException(number));

        // Get current draw order
        long currentDrawCount = drawRepository.countByGameId(gameId);

        // Create draw
        DrawEntity draw = DrawEntity.builder()
                .game(game)
                .number(number)
                .word(wordMapping.getWord())
                .drawOrder((int) currentDrawCount + 1)
                .build();

        // Update game status to IN_PROGRESS if first draw
        if (game.getStatus() == GameEntity.GameStatus.CREATED) {
            game.setStatus(GameEntity.GameStatus.IN_PROGRESS);
            gameRepository.save(game);
        }

        return drawRepository.save(draw);
    }

    /**
     * Get all draws for a game.
     */
    @Transactional(readOnly = true)
    public List<DrawEntity> getDrawsByGame(UUID gameId) {
        return drawRepository.findByGameIdOrderByDrawOrderAsc(gameId);
    }

    /**
     * Get all drawn numbers for a game.
     */
    @Transactional(readOnly = true)
    public List<Integer> getDrawnNumbers(UUID gameId) {
        return drawRepository.findByGameIdOrderByDrawOrderAsc(gameId).stream()
                .map(DrawEntity::getNumber)
                .collect(Collectors.toList());
    }

    /**
     * Get all drawn words for a game.
     */
    @Transactional(readOnly = true)
    public List<String> getDrawnWords(UUID gameId) {
        return drawRepository.findByGameIdOrderByDrawOrderAsc(gameId).stream()
                .map(DrawEntity::getWord)
                .collect(Collectors.toList());
    }

    /**
     * Get remaining numbers for a game.
     */
    @Transactional(readOnly = true)
    public List<Integer> getRemainingNumbers(UUID gameId) {
        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameService.GameNotFoundException(gameId));

        List<Integer> drawnNumbers = getDrawnNumbers(gameId);

        return game.getWords().stream()
                .map(WordMappingEntity::getNumber)
                .filter(num -> !drawnNumbers.contains(num))
                .collect(Collectors.toList());
    }

    /**
     * Check if number is already drawn.
     */
    @Transactional(readOnly = true)
    public boolean isNumberDrawn(UUID gameId, Integer number) {
        return drawRepository.existsByGameIdAndNumber(gameId, number);
    }

    // Custom exceptions
    public static class NumberAlreadyDrawnException extends RuntimeException {
        public NumberAlreadyDrawnException(Integer number) {
            super("Number already drawn: " + number);
        }
    }

    public static class InvalidNumberException extends RuntimeException {
        public InvalidNumberException(Integer number) {
            super("Invalid number for this game: " + number);
        }
    }
}
