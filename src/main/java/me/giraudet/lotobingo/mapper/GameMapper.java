package me.giraudet.lotobingo.mapper;

import me.giraudet.lotobingo.dto.Game;
import me.giraudet.lotobingo.dto.GameCreationRequest;
import me.giraudet.lotobingo.dto.WordMapping;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.entity.WordMappingEntity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Mapper to convert between DTOs (from OpenAPI) and Entities (JPA).
 *
 * DTOs live in: target/generated-sources/.../dto/
 * Entities live in: src/.../entity/
 */
@Component
public class GameMapper {

    /**
     * Convert API request DTO to Entity for database storage.
     */
    public GameEntity toEntity(GameCreationRequest dto) {
        if (dto == null) {
            return null;
        }

        return GameEntity.builder()
                .numberOfCards(dto.getNumberOfCards())
                .wordsPerCard(dto.getWordsPerCard())
                .gridRows(dto.getGridSize() != null ? dto.getGridSize().getRows() : 3)
                .gridColumns(dto.getGridSize() != null ? dto.getGridSize().getColumns() : 5)
                .status(GameEntity.GameStatus.CREATED)
                .build();
    }

    /**
     * Convert Entity from database to API response DTO.
     */
    public Game toDto(GameEntity entity) {
        if (entity == null) {
            return null;
        }

        Game dto = new Game();
        dto.setId(entity.getId());
        dto.setNumberOfCards(entity.getNumberOfCards());
        dto.setWordsPerCard(entity.getWordsPerCard());

        // Map GridSize
        me.giraudet.lotobingo.dto.GridSize gridSize = new me.giraudet.lotobingo.dto.GridSize();
        gridSize.setRows(entity.getGridRows());
        gridSize.setColumns(entity.getGridColumns());
        dto.setGridSize(gridSize);

        // Map status
        dto.setStatus(Game.StatusEnum.fromValue(entity.getStatus().name().toLowerCase()));

        // Map created date
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(OffsetDateTime.of(entity.getCreatedAt(), ZoneOffset.UTC));
        }

        // Map word mappings
        if (entity.getWords() != null) {
            dto.setWords(entity.getWords().stream()
                    .map(this::wordMappingToDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Convert WordMapping entity to DTO.
     */
    private WordMapping wordMappingToDto(WordMappingEntity entity) {
        WordMapping dto = new WordMapping();
        dto.setNumber(entity.getNumber());
        dto.setWord(entity.getWord());
        return dto;
    }

    /**
     * Create WordMapping entity from word string.
     */
    public WordMappingEntity createWordMapping(GameEntity game, String word, int number) {
        return WordMappingEntity.builder()
                .game(game)
                .word(word)
                .number(number)
                .build();
    }
}
