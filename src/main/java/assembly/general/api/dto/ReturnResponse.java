package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ReturnResponse {
    private UUID reservationId;
    private LocalDateTime returnedAt;
    private long lateDays;
    private double lateFee;
    private String message;
}