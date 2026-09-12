package assembly.general.api.exception;

import lombok.Getter;

@Getter
public class BookUnavailableException extends RuntimeException {
    private final int availableCopies;

    public BookUnavailableException(String message, int availableCopies) {
        super(message);
        this.availableCopies = availableCopies;
    }
}