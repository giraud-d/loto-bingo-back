package me.giraudet.lotobingo.service;

import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import me.giraudet.lotobingo.mapper.GameMapper;
import me.giraudet.lotobingo.repository.GameRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameMapper gameMapper;

    @Mock
    private BingoCardService bingoCardService;

    @InjectMocks
    private GameService gameService;

    private GameEntity testGame;
    private List<String> testWords;

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

        testWords = Arrays.asList(
                "Apple", "Banana", "Cherry", "Date", "Elderberry",
                "Fig", "Grape", "Honeydew", "Kiwi", "Lemon"
        );
    }

    @Test
    void createGame_shouldCreateGameWithWords() {
        // Given
        GameEntity inputGame = GameEntity.builder()
                .numberOfCards(10)
                .wordsPerCard(15)
                .gridRows(3)
                .gridColumns(5)
                .words(new ArrayList<>())
                .build();

        when(gameRepository.save(any(GameEntity.class))).thenReturn(testGame);
        when(gameMapper.createWordMapping(any(), anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    GameEntity game = invocation.getArgument(0);
                    String word = invocation.getArgument(1);
                    Integer number = invocation.getArgument(2);
                    return WordMappingEntity.builder()
                            .game(game)
                            .word(word)
                            .number(number)
                            .build();
                });

        // When
        GameEntity result = gameService.createGame(inputGame, testWords);

        // Then
        assertThat(result).isNotNull();
        verify(gameRepository, times(2)).save(any(GameEntity.class));
        verify(bingoCardService).generateCards(any(GameEntity.class));
        assertThat(testGame.getWords()).hasSize(testWords.size());
    }

    @Test
    void createGame_withNullWords_shouldThrowException() {
        // Given
        GameEntity inputGame = GameEntity.builder()
                .numberOfCards(10)
                .wordsPerCard(15)
                .build();

        // When & Then
        assertThatThrownBy(() -> gameService.createGame(inputGame, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Words must be between 1 and 90");
    }

    @Test
    void createGame_withEmptyWords_shouldThrowException() {
        // Given
        GameEntity inputGame = GameEntity.builder()
                .numberOfCards(10)
                .wordsPerCard(15)
                .build();

        // When & Then
        assertThatThrownBy(() -> gameService.createGame(inputGame, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Words must be between 1 and 90");
    }

    @Test
    void createGame_withTooManyWords_shouldThrowException() {
        // Given
        GameEntity inputGame = GameEntity.builder()
                .numberOfCards(10)
                .wordsPerCard(15)
                .build();

        List<String> tooManyWords = new ArrayList<>();
        for (int i = 0; i < 91; i++) {
            tooManyWords.add("Word" + i);
        }

        // When & Then
        assertThatThrownBy(() -> gameService.createGame(inputGame, tooManyWords))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Words must be between 1 and 90");
    }

    @Test
    void findById_shouldReturnGame() {
        // Given
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));

        // When
        GameEntity result = gameService.findById(gameId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testGame.getId());
        verify(gameRepository).findById(gameId);
    }

    @Test
    void findById_withNonExistentId_shouldThrowException() {
        // Given
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.findById(gameId))
                .isInstanceOf(GameService.GameNotFoundException.class)
                .hasMessageContaining("Game not found with id: " + gameId);
    }

    @Test
    void updateStatus_shouldUpdateGameStatus() {
        // Given
        UUID gameId = UUID.randomUUID();
        testGame.setStatus(GameEntity.GameStatus.CREATED);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(GameEntity.class))).thenReturn(testGame);

        // When
        GameEntity result = gameService.updateStatus(gameId, GameEntity.GameStatus.IN_PROGRESS);

        // Then
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.IN_PROGRESS);
        verify(gameRepository).save(testGame);
    }

    @Test
    void updateStatus_withNonExistentGame_shouldThrowException() {
        // Given
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.updateStatus(gameId, GameEntity.GameStatus.IN_PROGRESS))
                .isInstanceOf(GameService.GameNotFoundException.class);
    }
}
