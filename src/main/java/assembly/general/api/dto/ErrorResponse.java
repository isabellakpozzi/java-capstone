package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private String timestamp;

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now().toString());
    }
}