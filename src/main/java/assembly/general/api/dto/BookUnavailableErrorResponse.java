package assembly.general.api.dto;

import lombok.Getter;

@Getter
public class BookUnavailableErrorResponse {
    private final String error = "BOOK_UNAVAILABLE";
    private final String message;
    private final int availableCopies;

    public BookUnavailableErrorResponse(String message, int availableCopies) {
        this.message = message;
        this.availableCopies = availableCopies;
    }
}