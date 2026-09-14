package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.Status;
import assembly.general.api.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository
        extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByBook(Book book);

    List<Reservation> findByStatus(Status status);

    List<Reservation> findByUserAndStatus(
            User user,
            Status status
    );

    List<Reservation> findByUserAndStatusInOrderByReservedAtDesc(
            User user,
            List<Status> statuses
    );

    Optional<Reservation> findByBookAndStatusIn(
            Book book,
            List<Status> statuses
    );

    long countByUserAndStatusIn(
            User user,
            List<Status> statuses
    );

    long countByUserAndStatus(
            User user,
            Status status
    );

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.user = :user
            ORDER BY COALESCE(r.returnedAt, r.reservedAt) DESC
            """)
    Page<Reservation> findHistoryByUser(
            @Param("user") User user,
            Pageable pageable
    );
}