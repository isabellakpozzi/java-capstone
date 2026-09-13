package assembly.general.api.service;

import assembly.general.api.dto.*;
import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.User;
import assembly.general.api.exception.*;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    @Transactional
    public CheckoutResponse checkout(UUID reservationId, CheckoutRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidReservationStatusException(
                    "Can only checkout reservations with RESERVED status", reservation.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(14);

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setCheckedOutAt(now);
        reservation.setDueDate(dueDate);
        reservation.setNotes(request.getNotes());

        Reservation saved = reservationRepository.save(reservation);

        String formattedDate = dueDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        return new CheckoutResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getCheckedOutAt(),
                saved.getDueDate(),
                "Book checked out successfully. Due date: " + formattedDate
        );
    }

    @Transactional
    public ReturnResponse returnBook(UUID reservationId, ReturnRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new InvalidReservationStatusException(
                    "Can only return reservations with CHECKED_OUT status", reservation.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        long lateDays = 0;
        if (now.isAfter(reservation.getDueDate())) {
            lateDays = ChronoUnit.DAYS.between(reservation.getDueDate(), now);
            if (lateDays < 1) {
                lateDays = 1; // any overdue amount under 24h still counts as 1 late day
            }
        }
        double lateFee = lateDays * 1.00;

        reservation.setStatus(ReservationStatus.RETURNED);
        reservation.setReturnedAt(now);
        reservation.setConditionAtReturn(request.getCondition());
        reservation.setNotes(request.getNotes());
        reservation.setLateDays((int) lateDays);
        reservation.setLateFee(lateFee);

        Book book = reservation.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        Reservation saved = reservationRepository.save(reservation);

        String message = lateDays > 0
                ? String.format("Book returned. Late fee of $%.2f applied to account.", lateFee)
                : "Book returned successfully";

        return new ReturnResponse(saved.getId(), saved.getReturnedAt(), lateDays, lateFee, message);
    }

    public PagedResponse<BorrowingHistoryItem> getHistory(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        // Most-recent-first: returnedAt covers RETURNED items, reservedAt covers
        // everything else (RESERVED/CHECKED_OUT/CANCELLED have no returnedAt yet).
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reservedAt"));

        Page<Reservation> result = reservationRepository.findByUser(user, pageable);

        List<BorrowingHistoryItem> content = result.getContent().stream()
                .map(BorrowingHistoryItem::new)
                .toList();

        return new PagedResponse<>(content, result);
    }
}