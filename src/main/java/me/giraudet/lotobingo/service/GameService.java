package me.giraudet.lotobingo.service;

import lombok.RequiredArgsConstructor;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import me.giraudet.lotobingo.mapper.GameMapper;
import me.giraudet.lotobingo.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for Game business logic.
 * Works with Entities (not DTOs).
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameMapper gameMapper;
    private final BingoCardService bingoCardService;

    /**
     * Create a new game with the given words.
     *
     * @param gameEntity The game entity (without ID)
     * @param words List of words to map to numbers
     * @return The saved game entity with ID
     */
    @Transactional
    public GameEntity createGame(GameEntity gameEntity, List<String> words) {
        // Validate words
        if (words == null || words.isEmpty() || words.size() > 90) {
            throw new IllegalArgumentException("Words must be between 1 and 90");
        }

        // Save the game first
        GameEntity savedGame = gameRepository.save(gameEntity);

        // Create word mappings
        for (int i = 0; i < words.size(); i++) {
            WordMappingEntity mapping = gameMapper.createWordMapping(
                    savedGame,
                    words.get(i),
                    i + 1 // Numbers start at 1
            );
            savedGame.getWords().add(mapping);
        }

        // Generate bingo cards
        bingoCardService.generateCards(savedGame);

        return gameRepository.save(savedGame);
    }

    /**
     * Find a game by ID.
     */
    @Transactional(readOnly = true)
    public GameEntity findById(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    /**
     * Update game status.
     */
    @Transactional
    public GameEntity updateStatus(UUID gameId, GameEntity.GameStatus newStatus) {
        GameEntity game = findById(gameId);
        game.setStatus(newStatus);
        return gameRepository.save(game);
    }

    // Custom exception
    public static class GameNotFoundException extends RuntimeException {
        public GameNotFoundException(UUID gameId) {
            super("Game not found with id: " + gameId);
        }
    }
}
