package me.giraudet.lotobingo.config;

import me.giraudet.lotobingo.dto.Error;
import me.giraudet.lotobingo.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for REST API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle game not found exceptions.
     */
    @ExceptionHandler(GameService.GameNotFoundException.class)
    public ResponseEntity<Error> handleGameNotFound(GameService.GameNotFoundException ex) {
        Error error = new Error();
        error.setCode("GAME_NOT_FOUND");
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle draw service exceptions (already drawn, invalid number, etc).
     */
    @ExceptionHandler({
        me.giraudet.lotobingo.service.DrawService.InvalidNumberException.class,
        me.giraudet.lotobingo.service.DrawService.NumberAlreadyDrawnException.class
    })
    public ResponseEntity<Error> handleDrawException(RuntimeException ex) {
        Error error = new Error();
        error.setCode("DRAW_ERROR");
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle 404 - No handler found.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Error> handleNoHandlerFound(NoHandlerFoundException ex) {
        Error error = new Error();
        error.setCode("NOT_FOUND");
        error.setMessage("The requested endpoint does not exist: " + ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleGenericException(Exception ex) {
        Error error = new Error();
        error.setCode("INTERNAL_ERROR");
        error.setMessage("An unexpected internal error occurred. Please contact support.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
