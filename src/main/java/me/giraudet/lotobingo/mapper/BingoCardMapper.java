package me.giraudet.lotobingo.mapper;

import me.giraudet.lotobingo.dto.*;
import me.giraudet.lotobingo.entity.BingoCardEntity;
import me.giraudet.lotobingo.entity.DrawEntity;
import me.giraudet.lotobingo.service.BingoCardService;
import me.giraudet.lotobingo.service.StatisticsService;
import me.giraudet.lotobingo.service.ValidationService;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for BingoCard and related DTOs.
 */
@Component
public class BingoCardMapper {

    /**
     * Convert BingoCardEntity to BingoCard DTO.
     */
    public BingoCard toDto(BingoCardEntity entity, List<List<BingoCardService.CardCellData>> grid) {
        if (entity == null) {
            return null;
        }

        BingoCard dto = new BingoCard();
        dto.setId(entity.getId().toString());
        dto.setGameId(entity.getGame().getId());
        dto.setUniqueIdentifier(entity.getUniqueIdentifier());

        // Convert grid
        if (grid != null) {
            List<List<CardCell>> dtoGrid = grid.stream()
                    .map(row -> row.stream()
                            .map(this::cellToDto)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            dto.setGrid(dtoGrid);
        }

        return dto;
    }

    /**
     * Convert CardCellData to CardCell DTO.
     */
    private CardCell cellToDto(BingoCardService.CardCellData cellData) {
        CardCell dto = new CardCell();
        dto.setNumber(org.openapitools.jackson.nullable.JsonNullable.of(cellData.number));
        dto.setWord(org.openapitools.jackson.nullable.JsonNullable.of(cellData.word));
        dto.setIsMarked(cellData.isMarked);
        return dto;
    }

    /**
     * Convert DrawEntity to DrawResult DTO.
     */
    public DrawResult drawToDto(DrawEntity draw, List<Integer> drawnNumbers, List<String> drawnWords) {
        DrawResult dto = new DrawResult();
        dto.setNumber(draw.getNumber());
        dto.setWord(draw.getWord());
        dto.setDrawnNumbers(drawnNumbers);
        dto.setDrawnWords(drawnWords);
        return dto;
    }

    /**
     * Convert ValidationService.ValidationResult to ValidationResult DTO.
     */
    public ValidationResult validationToDto(ValidationService.ValidationResult result) {
        ValidationResult dto = new ValidationResult();
        dto.setIsValid(result.isValid);
        dto.setCardId(result.cardId);
        dto.setWinType(result.winType.name().toLowerCase());

        if (result.winningPattern != null) {
            List<Position> positions = result.winningPattern.stream()
                    .map(this::positionToDto)
                    .collect(Collectors.toList());
            dto.setWinningPattern(positions);
        }

        return dto;
    }

    /**
     * Convert ValidationService.Position to Position DTO.
     */
    private Position positionToDto(ValidationService.Position pos) {
        Position dto = new Position();
        dto.setRow(pos.row);
        dto.setColumn(pos.column);
        return dto;
    }

    /**
     * Convert GameStatistics to DTO.
     */
    public GameStatistics statisticsToDto(StatisticsService.GameStatistics stats) {
        GameStatistics dto = new GameStatistics();
        dto.setTotalCards(stats.totalCards);
        dto.setDrawCount(stats.drawCount);
        dto.setProbabilityFirstWin((float) stats.probabilityFirstWin);
        dto.setExpectedWinners((float) stats.expectedWinners);

        if (stats.probabilityByDraw != null) {
            List<DrawProbability> probs = stats.probabilityByDraw.stream()
                    .map(this::drawProbToDto)
                    .collect(Collectors.toList());
            dto.setProbabilityByDraw(probs);
        }

        return dto;
    }

    /**
     * Convert DrawProbability to DTO.
     */
    private DrawProbability drawProbToDto(StatisticsService.DrawProbability prob) {
        DrawProbability dto = new DrawProbability();
        dto.setDrawNumber(prob.drawNumber);
        dto.setCumulativeProbability((float) prob.cumulativeProbability);
        return dto;
    }

    /**
     * Convert game status to GameStatus DTO.
     */
    public GameStatus gameStatusToDto(
            java.util.UUID gameId,
            String status,
            int drawnCount,
            List<Integer> remainingNumbers,
            List<BingoCardEntity> winners
    ) {
        GameStatus dto = new GameStatus();
        dto.setGameId(gameId);
        dto.setStatus(GameStatus.StatusEnum.fromValue(status));
        dto.setDrawnCount(drawnCount);
        dto.setRemainingNumbers(remainingNumbers);

        if (winners != null && !winners.isEmpty()) {
            List<Winner> winnerDtos = winners.stream()
                    .map(this::winnerToDto)
                    .collect(Collectors.toList());
            dto.setWinners(winnerDtos);
        }

        return dto;
    }

    /**
     * Convert BingoCardEntity to Winner DTO.
     */
    private Winner winnerToDto(BingoCardEntity card) {
        Winner dto = new Winner();
        dto.setCardId(card.getId().toString());
        dto.setUniqueIdentifier(card.getUniqueIdentifier());
        dto.setWinType("full_card"); // Default for now
        // Timestamp would need to be tracked separately
        dto.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        return dto;
    }
}
