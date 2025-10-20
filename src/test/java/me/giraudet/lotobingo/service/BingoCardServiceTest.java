package me.giraudet.lotobingo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.giraudet.lotobingo.entity.BingoCardEntity;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import me.giraudet.lotobingo.repository.BingoCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BingoCardServiceTest {

    @Mock
    private BingoCardRepository bingoCardRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BingoCardService bingoCardService;

    private GameEntity testGame;
    private BingoCardEntity testCard;

    @BeforeEach
    void setUp() {
        testGame = GameEntity.builder()
                .id(UUID.randomUUID())
                .numberOfCards(10)
                .wordsPerCard(15)
                .gridRows(3)
                .gridColumns(5)
                .status(GameEntity.GameStatus.CREATED)
                .words(new ArrayList<>())
                .build();

        // Add word mappings
        for (int i = 1; i <= 20; i++) {
            WordMappingEntity word = WordMappingEntity.builder()
                    .id((long) i)
                    .game(testGame)
                    .number(i)
                    .word("Word" + i)
                    .build();
            testGame.getWords().add(word);
        }

        testCard = BingoCardEntity.builder()
                .id(UUID.randomUUID())
                .game(testGame)
                .uniqueIdentifier("TEST-0001")
                .gridData("{}")
                .hasWon(false)
                .build();
    }

    @Test
    void generateCards_shouldCreateCorrectNumberOfCards() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(bingoCardRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<BingoCardEntity> result = bingoCardService.generateCards(testGame);

        // Then
        assertThat(result).hasSize(testGame.getNumberOfCards());
        verify(bingoCardRepository).saveAll(anyList());
    }

    @Test
    void getCardsByGame_shouldReturnCards() {
        // Given
        UUID gameId = UUID.randomUUID();
        List<BingoCardEntity> cards = Arrays.asList(testCard);
        when(bingoCardRepository.findByGameId(gameId)).thenReturn(cards);

        // When
        List<BingoCardEntity> result = bingoCardService.getCardsByGame(gameId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testCard);
        verify(bingoCardRepository).findByGameId(gameId);
    }

    @Test
    void findByUniqueIdentifier_shouldReturnCard() {
        // Given
        String identifier = "TEST-0001";
        when(bingoCardRepository.findByUniqueIdentifier(identifier))
                .thenReturn(Optional.of(testCard));

        // When
        BingoCardEntity result = bingoCardService.findByUniqueIdentifier(identifier);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUniqueIdentifier()).isEqualTo(identifier);
        verify(bingoCardRepository).findByUniqueIdentifier(identifier);
    }

    @Test
    void findByUniqueIdentifier_withNonExistent_shouldThrowException() {
        // Given
        String identifier = "NONEXISTENT";
        when(bingoCardRepository.findByUniqueIdentifier(identifier))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bingoCardService.findByUniqueIdentifier(identifier))
                .isInstanceOf(BingoCardService.CardNotFoundException.class)
                .hasMessageContaining("Card not found with identifier: " + identifier);
    }

    @Test
    void markAsWinner_shouldUpdateCard() {
        // Given
        UUID cardId = testCard.getId();
        testCard.setHasWon(false);

        when(bingoCardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(bingoCardRepository.save(any(BingoCardEntity.class))).thenReturn(testCard);

        // When
        BingoCardEntity result = bingoCardService.markAsWinner(cardId);

        // Then
        assertThat(result.getHasWon()).isTrue();
        verify(bingoCardRepository).save(testCard);
    }

    @Test
    void markAsWinner_withNonExistentCard_shouldThrowException() {
        // Given
        UUID cardId = UUID.randomUUID();
        when(bingoCardRepository.findById(cardId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bingoCardService.markAsWinner(cardId))
                .isInstanceOf(BingoCardService.CardNotFoundException.class);
    }

    @Test
    void getCardGrid_shouldParseGridData() throws Exception {
        // This test requires complex mocking of Jackson TypeReference
        // For simplicity, we'll test with a real ObjectMapper in integration tests
        // Here we just verify the method can be called without NPE when properly mocked

        // Given
        String gridJson = "[[{\"number\":1,\"word\":\"Word1\",\"isMarked\":false}]]";
        testCard.setGridData(gridJson);

        com.fasterxml.jackson.databind.type.TypeFactory typeFactory =
                com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance();

        when(objectMapper.getTypeFactory()).thenReturn(typeFactory);

        List<List<BingoCardService.CardCellData>> expectedGrid = new ArrayList<>();
        List<BingoCardService.CardCellData> row = new ArrayList<>();
        row.add(new BingoCardService.CardCellData(1, "Word1", false));
        expectedGrid.add(row);

        when(objectMapper.readValue(eq(gridJson), any(com.fasterxml.jackson.databind.type.CollectionType.class)))
                .thenReturn(expectedGrid);

        // When
        List<List<BingoCardService.CardCellData>> result = bingoCardService.getCardGrid(testCard);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(1);
        assertThat(result.get(0).get(0).number).isEqualTo(1);
        assertThat(result.get(0).get(0).word).isEqualTo("Word1");
    }
}
