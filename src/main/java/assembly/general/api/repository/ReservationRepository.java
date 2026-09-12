package assembly.general.api.repository;

import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // active reservations and the 5-reservation limit check
    List<Reservation> findByUserAndStatusIn(User user, List<ReservationStatus> statuses);

    long countByUserAndStatusIn(User user, List<ReservationStatus> statuses);

    // full borrowing history sorted most-recent-first via Pageable
    Page<Reservation> findByUser(User user, Pageable pageable);
}