package assembly.general.api.exception;

public class ReservationNotFoundException extends RuntimeException {
  public ReservationNotFoundException(String message) {
    super(message);
  }
}