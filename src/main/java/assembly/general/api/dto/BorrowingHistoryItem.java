package assembly.general.api.dto;

import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class BorrowingHistoryItem {
    private final UUID reservationId;
    private final String bookTitle;
    private final String bookAuthor;
    private final LocalDateTime reservedAt;
    private final LocalDateTime checkedOutAt;
    private final LocalDateTime returnedAt;
    private final LocalDateTime dueDate;
    private final ReservationStatus status;
    private final boolean wasLate;

    public BorrowingHistoryItem(Reservation reservation) {
        this.reservationId = reservation.getId();
        this.bookTitle = reservation.getBook().getTitle();
        this.bookAuthor = reservation.getBook().getAuthor();
        this.reservedAt = reservation.getReservedAt();
        this.checkedOutAt = reservation.getCheckedOutAt();
        this.returnedAt = reservation.getReturnedAt();
        this.dueDate = reservation.getDueDate();
        this.status = reservation.getStatus();

        this.wasLate = reservation.getReturnedAt() != null
                && reservation.getDueDate() != null
                && reservation.getReturnedAt().isAfter(reservation.getDueDate());
    }
}