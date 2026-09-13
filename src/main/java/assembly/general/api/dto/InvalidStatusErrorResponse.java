package assembly.general.api.dto;

import assembly.general.api.entity.ReservationStatus;
import lombok.Getter;

@Getter
public class InvalidStatusErrorResponse {
    private final String error = "INVALID_STATUS";
    private final String message;
    private final ReservationStatus currentStatus;

    public InvalidStatusErrorResponse(String message, ReservationStatus currentStatus) {
        this.message = message;
        this.currentStatus = currentStatus;
    }
}