package assembly.general.api.exception;

import assembly.general.api.dto.BookUnavailableErrorResponse;
import assembly.general.api.dto.ErrorResponse;
import assembly.general.api.dto.InvalidStatusErrorResponse;
import assembly.general.api.dto.ReservationLimitErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * catches exceptions thrown anywhere in the controller layer and converts
 * them into the consistent {error, message, timestamp} JSON shape with the correct HTTP status code for each case.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthFailed(AuthenticationFailedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("AUTHENTICATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReservationLimitExceededException.class)
    public ResponseEntity<ReservationLimitErrorResponse> handleLimitExceeded(ReservationLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ReservationLimitErrorResponse(ex.getMessage(), ex.getCurrentReservations()));
    }

    @ExceptionHandler(BookUnavailableException.class)
    public ResponseEntity<BookUnavailableErrorResponse> handleBookUnavailable(BookUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookUnavailableErrorResponse(ex.getMessage(), ex.getAvailableCopies()));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(ReservationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidReservationStatusException.class)
    public ResponseEntity<InvalidStatusErrorResponse> handleInvalidStatus(InvalidReservationStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new InvalidStatusErrorResponse(ex.getMessage(), ex.getCurrentStatus()));
    }

    // triggered automatically when @Valid fails on a @RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", message));
    }
}