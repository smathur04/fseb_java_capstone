package assembly.general.api.repository;
import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.Status;
import assembly.general.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByBook(Book book);

    List<Reservation> findByStatus(Status status);

    List<Reservation> findByUserAndStatus(User user, Status status);

    Optional<Reservation> findByBookAndStatusIn(Book book, List<Status> statuses);

    long countByUserAndStatusIn(User user, List<Status> statuses);

    long countByUserAndStatus(User user, Status status);
}