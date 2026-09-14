package assembly.general.api.repository;

import assembly.general.api.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("patron@example.com");
        user.setPassword("hashed");
        user.setFirstName("Pat");
        user.setLastName("Ron");
        user.setPhoneNumber("+1-555-0100");
        user.setRole(Role.PATRON);
        user.setMembershipStatus(MembershipStatus.ACTIVE);
        user.setMemberSince(LocalDateTime.now());
        user = userRepository.save(user);

        book = new Book();
        book.setIsbn("999");
        book.setTitle("Test Book");
        book.setAuthor("Some Author");
        book.setGenre("Fiction");
        book.setPublicationYear(2020);
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        book = bookRepository.save(book);
    }

    private Reservation buildReservation(ReservationStatus status) {
        Reservation r = new Reservation();
        r.setUser(user);
        r.setBook(book);
        r.setStatus(status);
        r.setReservedAt(LocalDateTime.now());
        r.setExpiresAt(LocalDateTime.now().plusDays(7));
        return r;
    }

    @Test
    void findByUserAndStatusIn_returnsOnlyActiveReservations() {
        reservationRepository.save(buildReservation(ReservationStatus.RESERVED));
        reservationRepository.save(buildReservation(ReservationStatus.CHECKED_OUT));
        reservationRepository.save(buildReservation(ReservationStatus.RETURNED));

        List<Reservation> active = reservationRepository.findByUserAndStatusIn(
                user, List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT)
        );

        assertThat(active).hasSize(2);
    }

    @Test
    void countByUserAndStatusIn_matchesActiveCountUsedForLimitCheck() {
        reservationRepository.save(buildReservation(ReservationStatus.RESERVED));
        reservationRepository.save(buildReservation(ReservationStatus.RESERVED));
        reservationRepository.save(buildReservation(ReservationStatus.RETURNED)); // not active

        long count = reservationRepository.countByUserAndStatusIn(
                user, List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT)
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByUser_returnsFullHistoryIncludingReturnedAndCancelled() {
        reservationRepository.save(buildReservation(ReservationStatus.RETURNED));
        reservationRepository.save(buildReservation(ReservationStatus.CANCELLED));
        reservationRepository.save(buildReservation(ReservationStatus.RESERVED));

        Page<Reservation> history = reservationRepository.findByUser(user, PageRequest.of(0, 20));

        assertThat(history.getTotalElements()).isEqualTo(3);
    }

    @Test
    void countByUser_countsAllStatusesForBorrowingHistoryStat() {
        reservationRepository.save(buildReservation(ReservationStatus.RETURNED));
        reservationRepository.save(buildReservation(ReservationStatus.RESERVED));

        assertThat(reservationRepository.countByUser(user)).isEqualTo(2);
    }

    @Test
    void findByUser_returnsEmptyPage_whenUserHasNoReservations() {
        Page<Reservation> history = reservationRepository.findByUser(user, PageRequest.of(0, 20));

        assertThat(history.getContent()).isEmpty();
        assertThat(history.getTotalElements()).isZero();
    }

    @Test
    void reservationResolvesAssociatedBookAndUser() {
        Reservation saved = reservationRepository.save(buildReservation(ReservationStatus.RESERVED));

        Reservation found = reservationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getBook().getTitle()).isEqualTo("Test Book");
        assertThat(found.getUser().getEmail()).isEqualTo("patron@example.com");
    }
}