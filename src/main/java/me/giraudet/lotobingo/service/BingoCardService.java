package me.giraudet.lotobingo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.giraudet.lotobingo.entity.BingoCardEntity;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import me.giraudet.lotobingo.repository.BingoCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for generating and managing bingo cards.
 */
@Service
@RequiredArgsConstructor
public class BingoCardService {

    private final BingoCardRepository bingoCardRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate bingo cards for a game.
     */
    @Transactional
    public List<BingoCardEntity> generateCards(GameEntity game) {
        List<BingoCardEntity> cards = new ArrayList<>();

        for (int i = 0; i < game.getNumberOfCards(); i++) {
            BingoCardEntity card = generateSingleCard(game, i + 1);
            cards.add(card);
        }

        return bingoCardRepository.saveAll(cards);
    }

    /**
     * Generate a single bingo card.
     */
    private BingoCardEntity generateSingleCard(GameEntity game, int cardNumber) {
        // Select random words for this card
        List<WordMappingEntity> allWords = new ArrayList<>(game.getWords());
        Collections.shuffle(allWords);

        int wordsToSelect = Math.min(game.getWordsPerCard(), allWords.size());
        List<WordMappingEntity> selectedWords = allWords.subList(0, wordsToSelect);

        // Create grid
        List<List<CardCellData>> grid = createGrid(selectedWords, game.getGridRows(), game.getGridColumns());

        // Convert grid to JSON
        String gridJson;
        try {
            gridJson = objectMapper.writeValueAsString(grid);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize grid", e);
        }

        return BingoCardEntity.builder()
                .game(game)
                .uniqueIdentifier(generateUniqueIdentifier(game.getId(), cardNumber))
                .gridData(gridJson)
                .hasWon(false)
                .build();
    }

    /**
     * Create grid layout.
     */
    private List<List<CardCellData>> createGrid(List<WordMappingEntity> words, int rows, int columns) {
        List<List<CardCellData>> grid = new ArrayList<>();
        List<WordMappingEntity> shuffledWords = new ArrayList<>(words);
        Collections.shuffle(shuffledWords);

        int wordIndex = 0;
        for (int row = 0; row < rows; row++) {
            List<CardCellData> rowData = new ArrayList<>();
            for (int col = 0; col < columns; col++) {
                if (wordIndex < shuffledWords.size()) {
                    WordMappingEntity word = shuffledWords.get(wordIndex++);
                    rowData.add(new CardCellData(word.getNumber(), word.getWord(), false));
                } else {
                    // Empty cell if not enough words
                    rowData.add(new CardCellData(null, null, false));
                }
            }
            grid.add(rowData);
        }

        return grid;
    }

    /**
     * Generate unique identifier for card.
     */
    private String generateUniqueIdentifier(UUID gameId, int cardNumber) {
        return String.format("%s-%04d",
                gameId.toString().substring(0, 8).toUpperCase(),
                cardNumber);
    }

    /**
     * Get all cards for a game.
     */
    @Transactional(readOnly = true)
    public List<BingoCardEntity> getCardsByGame(UUID gameId) {
        return bingoCardRepository.findByGameId(gameId);
    }

    /**
     * Find card by unique identifier.
     */
    @Transactional(readOnly = true)
    public BingoCardEntity findByUniqueIdentifier(String uniqueIdentifier) {
        return bingoCardRepository.findByUniqueIdentifier(uniqueIdentifier)
                .orElseThrow(() -> new CardNotFoundException(uniqueIdentifier));
    }

    /**
     * Mark card as winner.
     */
    @Transactional
    public BingoCardEntity markAsWinner(UUID cardId) {
        BingoCardEntity card = bingoCardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId.toString()));
        card.setHasWon(true);
        return bingoCardRepository.save(card);
    }

    /**
     * Get card grid data.
     */
    public List<List<CardCellData>> getCardGrid(BingoCardEntity card) {
        try {
            return objectMapper.readValue(
                    card.getGridData(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, CardCellData.class)
                    )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse grid data", e);
        }
    }

    // Inner class for grid cell data
    public static class CardCellData {
        public Integer number;
        public String word;
        public boolean isMarked;

        public CardCellData() {}

        public CardCellData(Integer number, String word, boolean isMarked) {
            this.number = number;
            this.word = word;
            this.isMarked = isMarked;
        }
    }

    // Custom exception
    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException(String identifier) {
            super("Card not found with identifier: " + identifier);
        }
    }
}
