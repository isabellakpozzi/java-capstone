package assembly.general.api.dto;

import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
public class ActiveReservationItem {
    private final UUID reservationId;
    private final UUID bookId;
    private final String bookTitle;
    private final String bookAuthor;
    private final ReservationStatus status;

    // only populated when status == RESERVED
    private final LocalDateTime reservedAt;
    private final LocalDateTime expiresAt;
    private final Long daysUntilExpiry;

    // only populated when status == CHECKED_OUT
    private final LocalDateTime checkedOutAt;
    private final LocalDateTime dueDate;
    private final Long daysUntilDue;

    public ActiveReservationItem(Reservation reservation) {
        this.reservationId = reservation.getId();
        this.bookId = reservation.getBook().getId();
        this.bookTitle = reservation.getBook().getTitle();
        this.bookAuthor = reservation.getBook().getAuthor();
        this.status = reservation.getStatus();

        LocalDateTime now = LocalDateTime.now();

        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            this.reservedAt = reservation.getReservedAt();
            this.expiresAt = reservation.getExpiresAt();
            this.daysUntilExpiry = ChronoUnit.DAYS.between(now, reservation.getExpiresAt());
            this.checkedOutAt = null;
            this.dueDate = null;
            this.daysUntilDue = null;
        } else {
            // CHECKED_OUT
            this.reservedAt = null;
            this.expiresAt = null;
            this.daysUntilExpiry = null;
            this.checkedOutAt = reservation.getCheckedOutAt();
            this.dueDate = reservation.getDueDate();
            this.daysUntilDue = ChronoUnit.DAYS.between(now, reservation.getDueDate());
        }
    }
}