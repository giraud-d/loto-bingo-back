package me.giraudet.lotobingo.service;

import me.giraudet.lotobingo.entity.DrawEntity;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import me.giraudet.lotobingo.repository.DrawRepository;
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
class DrawServiceTest {

    @Mock
    private DrawRepository drawRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private DrawService drawService;

    private GameEntity testGame;
    private DrawEntity testDraw;

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
        for (int i = 1; i <= 10; i++) {
            WordMappingEntity word = WordMappingEntity.builder()
                    .id((long) i)
                    .game(testGame)
                    .number(i)
                    .word("Word" + i)
                    .build();
            testGame.getWords().add(word);
        }

        testDraw = DrawEntity.builder()
                .id(1L)
                .game(testGame)
                .number(1)
                .word("Word1")
                .drawOrder(1)
                .build();
    }

    @Test
    void drawNumber_shouldCreateDraw() {
        // Given
        UUID gameId = testGame.getId();
        Integer number = 1;

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(drawRepository.existsByGameIdAndNumber(gameId, number)).thenReturn(false);
        when(drawRepository.countByGameId(gameId)).thenReturn(0L);
        when(drawRepository.save(any(DrawEntity.class))).thenReturn(testDraw);

        // When
        DrawEntity result = drawService.drawNumber(gameId, number);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(number);
        assertThat(result.getWord()).isEqualTo("Word1");
        verify(drawRepository).save(any(DrawEntity.class));
        verify(gameRepository).save(testGame); // Status should be updated to IN_PROGRESS
    }

    @Test
    void drawNumber_withNonExistentGame_shouldThrowException() {
        // Given
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> drawService.drawNumber(gameId, 1))
                .isInstanceOf(GameService.GameNotFoundException.class);
    }

    @Test
    void drawNumber_withAlreadyDrawnNumber_shouldThrowException() {
        // Given
        UUID gameId = testGame.getId();
        Integer number = 1;

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(drawRepository.existsByGameIdAndNumber(gameId, number)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> drawService.drawNumber(gameId, number))
                .isInstanceOf(DrawService.NumberAlreadyDrawnException.class)
                .hasMessageContaining("Number already drawn: " + number);
    }

    @Test
    void drawNumber_withInvalidNumber_shouldThrowException() {
        // Given
        UUID gameId = testGame.getId();
        Integer invalidNumber = 99;

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(drawRepository.existsByGameIdAndNumber(gameId, invalidNumber)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> drawService.drawNumber(gameId, invalidNumber))
                .isInstanceOf(DrawService.InvalidNumberException.class)
                .hasMessageContaining("Invalid number for this game: " + invalidNumber);
    }

    @Test
    void getDrawsByGame_shouldReturnDraws() {
        // Given
        UUID gameId = testGame.getId();
        List<DrawEntity> draws = Arrays.asList(testDraw);

        when(drawRepository.findByGameIdOrderByDrawOrderAsc(gameId)).thenReturn(draws);

        // When
        List<DrawEntity> result = drawService.getDrawsByGame(gameId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testDraw);
        verify(drawRepository).findByGameIdOrderByDrawOrderAsc(gameId);
    }

    @Test
    void getDrawnNumbers_shouldReturnNumbers() {
        // Given
        UUID gameId = testGame.getId();
        List<DrawEntity> draws = Arrays.asList(testDraw);

        when(drawRepository.findByGameIdOrderByDrawOrderAsc(gameId)).thenReturn(draws);

        // When
        List<Integer> result = drawService.getDrawnNumbers(gameId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(testDraw.getNumber());
    }

    @Test
    void getDrawnWords_shouldReturnWords() {
        // Given
        UUID gameId = testGame.getId();
        List<DrawEntity> draws = Arrays.asList(testDraw);

        when(drawRepository.findByGameIdOrderByDrawOrderAsc(gameId)).thenReturn(draws);

        // When
        List<String> result = drawService.getDrawnWords(gameId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(testDraw.getWord());
    }

    @Test
    void getRemainingNumbers_shouldReturnUndrawnNumbers() {
        // Given
        UUID gameId = testGame.getId();
        List<DrawEntity> draws = Arrays.asList(testDraw);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(drawRepository.findByGameIdOrderByDrawOrderAsc(gameId)).thenReturn(draws);

        // When
        List<Integer> result = drawService.getRemainingNumbers(gameId);

        // Then
        assertThat(result).hasSize(9); // 10 total - 1 drawn
        assertThat(result).doesNotContain(testDraw.getNumber());
    }

    @Test
    void isNumberDrawn_shouldReturnTrue_whenDrawn() {
        // Given
        UUID gameId = testGame.getId();
        Integer number = 1;

        when(drawRepository.existsByGameIdAndNumber(gameId, number)).thenReturn(true);

        // When
        boolean result = drawService.isNumberDrawn(gameId, number);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isNumberDrawn_shouldReturnFalse_whenNotDrawn() {
        // Given
        UUID gameId = testGame.getId();
        Integer number = 1;

        when(drawRepository.existsByGameIdAndNumber(gameId, number)).thenReturn(false);

        // When
        boolean result = drawService.isNumberDrawn(gameId, number);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void drawNumber_shouldUpdateStatusToInProgress_onFirstDraw() {
        // Given
        UUID gameId = testGame.getId();
        Integer number = 1;
        testGame.setStatus(GameEntity.GameStatus.CREATED);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(drawRepository.existsByGameIdAndNumber(gameId, number)).thenReturn(false);
        when(drawRepository.countByGameId(gameId)).thenReturn(0L);
        when(drawRepository.save(any(DrawEntity.class))).thenReturn(testDraw);

        // When
        drawService.drawNumber(gameId, number);

        // Then
        assertThat(testGame.getStatus()).isEqualTo(GameEntity.GameStatus.IN_PROGRESS);
        verify(gameRepository).save(testGame);
    }
}
