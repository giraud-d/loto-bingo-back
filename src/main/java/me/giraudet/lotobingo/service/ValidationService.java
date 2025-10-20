package me.giraudet.lotobingo.service;

import lombok.RequiredArgsConstructor;
import me.giraudet.lotobingo.entity.BingoCardEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for validating bingo card wins.
 */
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final BingoCardService bingoCardService;
    private final DrawService drawService;

    /**
     * Validate if a card has won.
     */
    @Transactional
    public ValidationResult validateCard(UUID gameId, String cardId, WinType winType) {
        // Find card
        BingoCardEntity card = bingoCardService.findByUniqueIdentifier(cardId);

        // Verify card belongs to this game
        if (!card.getGame().getId().equals(gameId)) {
            throw new InvalidCardException("Card does not belong to this game");
        }

        // Get drawn numbers
        List<Integer> drawnNumbers = drawService.getDrawnNumbers(gameId);

        // Get card grid
        List<List<BingoCardService.CardCellData>> grid = bingoCardService.getCardGrid(card);

        // Validate based on win type
        List<Position> winningPattern = switch (winType) {
            case LINE -> validateLine(grid, drawnNumbers);
            case FULL_CARD -> validateFullCard(grid, drawnNumbers);
            case PATTERN -> validatePattern(grid, drawnNumbers);
        };

        boolean isValid = !winningPattern.isEmpty();

        // Mark card as winner if valid
        if (isValid) {
            bingoCardService.markAsWinner(card.getId());
        }

        return new ValidationResult(isValid, cardId, winType, winningPattern);
    }

    /**
     * Validate line win (any complete row or column).
     */
    private List<Position> validateLine(List<List<BingoCardService.CardCellData>> grid, List<Integer> drawnNumbers) {
        int rows = grid.size();
        int cols = grid.isEmpty() ? 0 : grid.get(0).size();

        // Check rows
        for (int row = 0; row < rows; row++) {
            List<Position> rowPositions = new ArrayList<>();
            boolean rowComplete = true;

            for (int col = 0; col < cols; col++) {
                BingoCardService.CardCellData cell = grid.get(row).get(col);
                if (cell.number == null || !drawnNumbers.contains(cell.number)) {
                    rowComplete = false;
                    break;
                }
                rowPositions.add(new Position(row, col));
            }

            if (rowComplete) {
                return rowPositions;
            }
        }

        // Check columns
        for (int col = 0; col < cols; col++) {
            List<Position> colPositions = new ArrayList<>();
            boolean colComplete = true;

            for (int row = 0; row < rows; row++) {
                BingoCardService.CardCellData cell = grid.get(row).get(col);
                if (cell.number == null || !drawnNumbers.contains(cell.number)) {
                    colComplete = false;
                    break;
                }
                colPositions.add(new Position(row, col));
            }

            if (colComplete) {
                return colPositions;
            }
        }

        return new ArrayList<>();
    }

    /**
     * Validate full card win (all numbers drawn).
     */
    private List<Position> validateFullCard(List<List<BingoCardService.CardCellData>> grid, List<Integer> drawnNumbers) {
        List<Position> allPositions = new ArrayList<>();

        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < grid.get(row).size(); col++) {
                BingoCardService.CardCellData cell = grid.get(row).get(col);
                if (cell.number == null) {
                    continue; // Skip empty cells
                }
                if (!drawnNumbers.contains(cell.number)) {
                    return new ArrayList<>(); // Not all numbers drawn
                }
                allPositions.add(new Position(row, col));
            }
        }

        return allPositions;
    }

    /**
     * Validate pattern win (customizable pattern).
     * For now, same as line validation.
     */
    private List<Position> validatePattern(List<List<BingoCardService.CardCellData>> grid, List<Integer> drawnNumbers) {
        return validateLine(grid, drawnNumbers);
    }

    // Inner classes
    public static class ValidationResult {
        public final boolean isValid;
        public final String cardId;
        public final WinType winType;
        public final List<Position> winningPattern;

        public ValidationResult(boolean isValid, String cardId, WinType winType, List<Position> winningPattern) {
            this.isValid = isValid;
            this.cardId = cardId;
            this.winType = winType;
            this.winningPattern = winningPattern;
        }
    }

    public static class Position {
        public final int row;
        public final int column;

        public Position(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    public enum WinType {
        LINE,
        FULL_CARD,
        PATTERN
    }

    // Custom exception
    public static class InvalidCardException extends RuntimeException {
        public InvalidCardException(String message) {
            super(message);
        }
    }
}
