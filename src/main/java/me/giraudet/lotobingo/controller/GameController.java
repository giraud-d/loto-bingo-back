package me.giraudet.lotobingo.controller;

import lombok.RequiredArgsConstructor;
import me.giraudet.lotobingo.api.DefaultApi;
import me.giraudet.lotobingo.dto.*;
import me.giraudet.lotobingo.entity.GameEntity;
import me.giraudet.lotobingo.mapper.BingoCardMapper;
import me.giraudet.lotobingo.mapper.GameMapper;
import me.giraudet.lotobingo.service.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller that implements the generated API interface.
 *
 * The GamesApi interface is generated from open-api.yaml
 * Location: target/generated-sources/openapi/.../api/GamesApi.java
 *
 * This controller:
 * 1. Receives DTOs from the API layer
 * 2. Uses Mapper to convert DTOs to Entities
 * 3. Calls Service layer with Entities
 * 4. Converts result Entities back to DTOs
 * 5. Returns DTOs to client
 */
@RestController
@RequiredArgsConstructor
public class GameController implements DefaultApi {

    private final GameService gameService;
    private final GameMapper gameMapper;
    private final BingoCardService bingoCardService;
    private final BingoCardMapper bingoCardMapper;
    private final DrawService drawService;
    private final ValidationService validationService;
    private final StatisticsService statisticsService;

    /**
     * POST /games - Create a new game
     * Implements the interface method from DefaultApi (generated).
     */
    @Override
    public ResponseEntity<Game> gamesPost(GameCreationRequest gameCreationRequest) {
        // 1. Convert DTO to Entity
        GameEntity gameEntity = gameMapper.toEntity(gameCreationRequest);

        // 2. Call service layer (business logic)
        GameEntity savedGame = gameService.createGame(
                gameEntity,
                gameCreationRequest.getWords()
        );

        // 3. Convert Entity back to DTO
        Game responseDto = gameMapper.toDto(savedGame);

        // 4. Return response
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * GET /games/{gameId} - Get game details
     */
    @Override
    public ResponseEntity<Game> gamesGameIdGet(UUID gameId) {
        // 1. Call service
        GameEntity game = gameService.findById(gameId);

        // 2. Convert to DTO
        Game dto = gameMapper.toDto(game);

        // 3. Return
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /games/{gameId}/cards - Get all bingo cards for a game
     */
    @Override
    public ResponseEntity<List<BingoCard>> gamesGameIdCardsGet(UUID gameId) {
        List<me.giraudet.lotobingo.entity.BingoCardEntity> cards = bingoCardService.getCardsByGame(gameId);

        List<BingoCard> dtos = cards.stream()
                .map(card -> {
                    List<List<me.giraudet.lotobingo.service.BingoCardService.CardCellData>> grid =
                            bingoCardService.getCardGrid(card);
                    return bingoCardMapper.toDto(card, grid);
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /games/{gameId}/cards/pdf - Generate PDF with all bingo cards
     */
    @Override
    public ResponseEntity<Resource> gamesGameIdCardsPdfGet(UUID gameId, Integer cardsPerPage) {
        // TODO: Implementation pending
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * POST /games/{gameId}/draw - Draw a number/word
     */
    @Override
    public ResponseEntity<DrawResult> gamesGameIdDrawPost(UUID gameId, DrawRequest drawRequest) {
        me.giraudet.lotobingo.entity.DrawEntity draw = drawService.drawNumber(gameId, drawRequest.getNumber());

        List<Integer> drawnNumbers = drawService.getDrawnNumbers(gameId);
        List<String> drawnWords = drawService.getDrawnWords(gameId);

        DrawResult dto = bingoCardMapper.drawToDto(draw, drawnNumbers, drawnWords);

        return ResponseEntity.ok(dto);
    }

    /**
     * GET /games/{gameId}/statistics - Get game statistics
     */
    @Override
    public ResponseEntity<GameStatistics> gamesGameIdStatisticsGet(UUID gameId, Integer drawCount) {
        me.giraudet.lotobingo.service.StatisticsService.GameStatistics stats =
                statisticsService.calculateStatistics(gameId, drawCount);

        GameStatistics dto = bingoCardMapper.statisticsToDto(stats);

        return ResponseEntity.ok(dto);
    }

    /**
     * GET /games/{gameId}/status - Get current game status
     */
    @Override
    public ResponseEntity<GameStatus> gamesGameIdStatusGet(UUID gameId) {
        GameEntity game = gameService.findById(gameId);

        int drawnCount = drawService.getDrawnNumbers(gameId).size();
        List<Integer> remainingNumbers = drawService.getRemainingNumbers(gameId);

        // Get winners (cards that have won)
        List<me.giraudet.lotobingo.entity.BingoCardEntity> winners = bingoCardService.getCardsByGame(gameId)
                .stream()
                .filter(me.giraudet.lotobingo.entity.BingoCardEntity::getHasWon)
                .collect(java.util.stream.Collectors.toList());

        GameStatus dto = bingoCardMapper.gameStatusToDto(
                gameId,
                game.getStatus().name().toLowerCase(),
                drawnCount,
                remainingNumbers,
                winners
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * POST /games/{gameId}/validate - Validate a winning card
     */
    @Override
    public ResponseEntity<ValidationResult> gamesGameIdValidatePost(UUID gameId, ValidationRequest validationRequest) {
        // Parse win type
        me.giraudet.lotobingo.service.ValidationService.WinType winType =
                me.giraudet.lotobingo.service.ValidationService.WinType.valueOf(
                        validationRequest.getWinType().name().toUpperCase()
                );

        me.giraudet.lotobingo.service.ValidationService.ValidationResult result =
                validationService.validateCard(gameId, validationRequest.getCardId(), winType);

        ValidationResult dto = bingoCardMapper.validationToDto(result);

        return ResponseEntity.ok(dto);
    }
}
