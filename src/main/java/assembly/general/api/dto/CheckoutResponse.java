package assembly.general.api.dto;

import assembly.general.api.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CheckoutResponse {
    private UUID reservationId;
    private ReservationStatus status;
    private LocalDateTime checkedOutAt;
    private LocalDateTime dueDate;
    private String message;
}