package assembly.general.api.dto;

import lombok.Getter;

@Getter
public class ReservationLimitErrorResponse {
    private final String error = "RESERVATION_LIMIT_EXCEEDED";
    private final String message;
    private final long currentReservations;

    public ReservationLimitErrorResponse(String message, long currentReservations) {
        this.message = message;
        this.currentReservations = currentReservations;
    }
}