package assembly.general.api.exception;

import assembly.general.api.entity.ReservationStatus;
import lombok.Getter;

@Getter
public class InvalidReservationStatusException extends RuntimeException {
    private final ReservationStatus currentStatus;

    public InvalidReservationStatusException(String message, ReservationStatus currentStatus) {
        super(message);
        this.currentStatus = currentStatus;
    }
}