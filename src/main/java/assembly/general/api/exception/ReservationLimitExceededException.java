package assembly.general.api.exception;

import lombok.Getter;

@Getter
public class ReservationLimitExceededException extends RuntimeException {
    private final long currentReservations;

    public ReservationLimitExceededException(String message, long currentReservations) {
        super(message);
        this.currentReservations = currentReservations;
    }
}