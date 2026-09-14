package assembly.general.api.service;

import assembly.general.api.dto.CheckoutRequest;
import assembly.general.api.dto.ReturnRequest;
import assembly.general.api.entity.*;
import assembly.general.api.exception.InvalidReservationStatusException;
import assembly.general.api.exception.ReservationNotFoundException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Book book;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(UUID.randomUUID());
        book.setTitle("Test Book");
        book.setAvailableCopies(2);

        User user = new User();
        user.setId(UUID.randomUUID());

        reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void returnBook_threeDaysLate_calculatesCorrectFee() {
        reservation.setDueDate(LocalDateTime.now().minusDays(3));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        var response = reservationService.returnBook(
                reservation.getId(), new ReturnRequestFixture(BookCondition.GOOD, null)
        );

        assertThat(response.getLateDays()).isEqualTo(3);
        assertThat(response.getLateFee()).isEqualTo(3.00);
        assertThat(response.getMessage()).contains("Late fee of $3.00");
    }

    @Test
    void returnBook_onTime_hasZeroLateFee() {
        reservation.setDueDate(LocalDateTime.now().plusDays(2)); // not yet due
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        var response = reservationService.returnBook(
                reservation.getId(), new ReturnRequestFixture(BookCondition.GOOD, null)
        );

        assertThat(response.getLateDays()).isZero();
        assertThat(response.getLateFee()).isEqualTo(0.00);
        assertThat(response.getMessage()).isEqualTo("Book returned successfully");
    }

    @Test
    void returnBook_incrementsAvailableCopies() {
        reservation.setDueDate(LocalDateTime.now().plusDays(1));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        reservationService.returnBook(reservation.getId(), new ReturnRequestFixture(BookCondition.GOOD, null));

        assertThat(book.getAvailableCopies()).isEqualTo(3); // was 2, now +1
    }

    @Test
    void returnBook_whenAlreadyReturned_throwsInvalidStatus() {
        reservation.setStatus(ReservationStatus.RETURNED);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                reservationService.returnBook(reservation.getId(), new ReturnRequestFixture(BookCondition.GOOD, null))
        ).isInstanceOf(InvalidReservationStatusException.class);
    }

    @Test
    void returnBook_whenReservationDoesNotExist_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(reservationRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.returnBook(randomId, new ReturnRequestFixture(BookCondition.GOOD, null))
        ).isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void checkout_whenNotReserved_throwsInvalidStatus() {
        reservation.setStatus(ReservationStatus.CHECKED_OUT); // already checked out
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                reservationService.checkout(reservation.getId(), new CheckoutRequest())
        ).isInstanceOf(InvalidReservationStatusException.class);
    }

    @Test
    void checkout_whenReservationDoesNotExist_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(reservationRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.checkout(randomId, new CheckoutRequest())
        ).isInstanceOf(ReservationNotFoundException.class);
    }

    private static class ReturnRequestFixture extends ReturnRequest {
        ReturnRequestFixture(BookCondition condition, String notes) {
            setCondition(condition);
            setNotes(notes);
        }
    }
}