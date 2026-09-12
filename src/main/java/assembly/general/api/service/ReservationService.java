package assembly.general.api.service;

import assembly.general.api.dto.ActiveReservationItem;
import assembly.general.api.dto.ActiveReservationsResponse;
import assembly.general.api.dto.ReservationRequest;
import assembly.general.api.dto.ReservationResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.User;
import assembly.general.api.exception.BookNotFoundException;
import assembly.general.api.exception.BookUnavailableException;
import assembly.general.api.exception.ReservationLimitExceededException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private static final int MAX_ACTIVE_RESERVATIONS = 5;
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            BookRepository bookRepository,
            UserRepository userRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse reserveBook(UUID userId, ReservationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        long activeCount = reservationRepository.countByUserAndStatusIn(user, ACTIVE_STATUSES);
        if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
            throw new ReservationLimitExceededException(
                    "You have reached the maximum of 5 active reservations", activeCount
            );
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + request.getBookId()));

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException(
                    "No copies available for reservation", book.getAvailableCopies() == null ? 0 : book.getAvailableCopies()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setReservedAt(now);
        reservation.setExpiresAt(now.plusDays(7));

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Reservation saved = reservationRepository.save(reservation);

        return new ReservationResponse(
                saved.getId(),
                book.getId(),
                user.getId(),
                book.getTitle(),
                saved.getStatus(),
                saved.getReservedAt(),
                saved.getExpiresAt(),
                "Book reserved successfully. Please pick up within 7 days."
        );
    }

    public ActiveReservationsResponse getActiveReservations(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        List<Reservation> active = reservationRepository.findByUserAndStatusIn(user, ACTIVE_STATUSES);

        List<ActiveReservationItem> items = active.stream()
                .map(ActiveReservationItem::new)
                .toList();

        return new ActiveReservationsResponse(items, items.size());
    }
}