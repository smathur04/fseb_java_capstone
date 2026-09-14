package assembly.general.api;

import assembly.general.api.entity.*;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.BookSpecifications;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private User user;
    private Book cleanCode;
    private Book dune;
    private Book unavailableBook;

    @BeforeEach
    void setUp() {
        user = new User(
                "repo@example.com",
                "hashed-password",
                "Repo",
                "User",
                "9255551234"
        );

        entityManager.persist(user);

        cleanCode = new Book(
                "9780132350884",
                "Clean Code",
                "Robert C. Martin",
                "Technology",
                2008,
                "Clean code book",
                "Prentice Hall",
                464,
                "English",
                5,
                3
        );

        dune = new Book(
                "9780441013593",
                "Dune",
                "Frank Herbert",
                "Science Fiction",
                1965,
                "Science fiction novel",
                "Ace",
                688,
                "English",
                4,
                2
        );

        unavailableBook = new Book(
                "9780000000001",
                "Unavailable Systems",
                "Jane Martin",
                "Technology",
                2020,
                "Unavailable book",
                "Example Press",
                300,
                "English",
                2,
                0
        );

        entityManager.persist(cleanCode);
        entityManager.persist(dune);
        entityManager.persist(unavailableBook);

        entityManager.flush();
    }

    // =========================================================
    // USER REPOSITORY
    // =========================================================

    @Test
    void userRepository_shouldFindUserByEmail() {
        assertTrue(
                userRepository
                        .findByEmail(
                                "repo@example.com"
                        )
                        .isPresent()
        );
    }

    @Test
    void userRepository_shouldReturnEmptyForUnknownEmail() {
        assertTrue(
                userRepository
                        .findByEmail(
                                "missing@example.com"
                        )
                        .isEmpty()
        );
    }

    @Test
    void database_shouldEnforceUniqueEmail() {
        User duplicate =
                new User(
                        "repo@example.com",
                        "another-hash",
                        "Duplicate",
                        "User",
                        "9255559999"
                );

        assertThrows(
                RuntimeException.class,
                () -> {
                    entityManager.persist(duplicate);
                    entityManager.flush();
                }
        );
    }

    // =========================================================
    // BOOK REPOSITORY / SPECIFICATIONS
    // =========================================================

    @Test
    void bookRepository_shouldSearchTitle() {
        Specification<Book> spec =
                BookSpecifications
                        .titleOrAuthorContains(
                                "clean"
                        );

        Page<Book> page =
                bookRepository.findAll(
                        spec,
                        PageRequest.of(0, 20)
                );

        assertEquals(1, page.getTotalElements());
        assertEquals(
                "Clean Code",
                page.getContent()
                        .getFirst()
                        .getTitle()
        );
    }

    @Test
    void bookRepository_shouldSearchAuthor() {
        Specification<Book> spec =
                BookSpecifications
                        .titleOrAuthorContains(
                                "martin"
                        );

        Page<Book> page =
                bookRepository.findAll(
                        spec,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                page.getTotalElements()
        );
    }

    @Test
    void bookRepository_shouldFilterGenre() {
        Page<Book> page =
                bookRepository.findAll(
                        BookSpecifications.hasGenre(
                                "Technology"
                        ),
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                page.getTotalElements()
        );
    }

    @Test
    void bookRepository_shouldFilterExactIsbn() {
        Page<Book> page =
                bookRepository.findAll(
                        BookSpecifications.hasIsbn(
                                "9780441013593"
                        ),
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                page.getTotalElements()
        );

        assertEquals(
                "Dune",
                page.getContent()
                        .getFirst()
                        .getTitle()
        );
    }

    @Test
    void bookRepository_shouldFilterAvailableOnly() {
        Page<Book> page =
                bookRepository.findAll(
                        BookSpecifications
                                .isAvailable(true),
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                page.getTotalElements()
        );

        assertTrue(
                page.getContent()
                        .stream()
                        .allMatch(
                                book ->
                                        book.getAvailableCopies() > 0
                        )
        );
    }

    @Test
    void bookRepository_shouldCombineFilters() {
        Specification<Book> specification =
                Specification.allOf(
                        BookSpecifications
                                .titleOrAuthorContains(
                                        "martin"
                                ),
                        BookSpecifications
                                .hasGenre(
                                        "Technology"
                                ),
                        BookSpecifications
                                .isAvailable(true)
                );

        Page<Book> page =
                bookRepository.findAll(
                        specification,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                page.getTotalElements()
        );

        assertEquals(
                "Clean Code",
                page.getContent()
                        .getFirst()
                        .getTitle()
        );
    }

    @Test
    void bookRepository_shouldSortAndPaginate() {
        Page<Book> page =
                bookRepository.findAll(
                        Specification.unrestricted(),
                        PageRequest.of(
                                0,
                                2,
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "title"
                                )
                        )
                );

        assertEquals(2, page.getSize());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());

        assertEquals(
                "Clean Code",
                page.getContent()
                        .getFirst()
                        .getTitle()
        );
    }

    @Test
    void database_shouldEnforceUniqueIsbn() {
        Book duplicate =
                new Book(
                        "9780132350884",
                        "Duplicate ISBN",
                        "Someone",
                        "Technology",
                        2024,
                        "Duplicate",
                        "Publisher",
                        100,
                        "English",
                        1,
                        1
                );

        assertThrows(
                RuntimeException.class,
                () -> {
                    entityManager.persist(duplicate);
                    entityManager.flush();
                }
        );
    }

    // =========================================================
    // RESERVATION REPOSITORY
    // =========================================================

    @Test
    void reservationRepository_shouldFindAndCountActiveReservations() {
        Reservation reserved =
                new Reservation(
                        cleanCode,
                        user,
                        Instant.now(),
                        null
                );

        Reservation checkedOut =
                new Reservation(
                        dune,
                        user,
                        Instant.now(),
                        null
                );

        checkedOut.checkOut(
                Instant.now(),
                null
        );

        entityManager.persist(reserved);
        entityManager.persist(checkedOut);
        entityManager.flush();

        List<Reservation> active =
                reservationRepository
                        .findByUserAndStatusInOrderByReservedAtDesc(
                                user,
                                List.of(
                                        Status.RESERVED,
                                        Status.CHECKED_OUT
                                )
                        );

        assertEquals(2, active.size());

        assertEquals(
                2,
                reservationRepository
                        .countByUserAndStatusIn(
                                user,
                                List.of(
                                        Status.RESERVED,
                                        Status.CHECKED_OUT
                                )
                        )
        );
    }

    @Test
    void reservationRepository_shouldReturnCompletePaginatedHistory() {
        Reservation returned =
                new Reservation(
                        cleanCode,
                        user,
                        Instant.now().minusSeconds(10000),
                        null
                );

        returned.checkOut(
                Instant.now().minusSeconds(9000),
                null
        );

        returned.returnBook(
                Instant.now(),
                Condition.GOOD,
                null
        );

        Reservation cancelled =
                new Reservation(
                        dune,
                        user,
                        Instant.now(),
                        null
                );

        cancelled.cancel(
                Instant.now(),
                "Changed mind",
                null
        );

        entityManager.persist(returned);
        entityManager.persist(cancelled);
        entityManager.flush();

        Page<Reservation> history =
                reservationRepository
                        .findHistoryByUser(
                                user,
                                PageRequest.of(
                                        0,
                                        20
                                )
                        );

        assertEquals(
                2,
                history.getTotalElements()
        );

        assertTrue(
                history.getContent()
                        .stream()
                        .anyMatch(
                                reservation ->
                                        reservation.getStatus()
                                                == Status.RETURNED
                        )
        );

        assertTrue(
                history.getContent()
                        .stream()
                        .anyMatch(
                                reservation ->
                                        reservation.getStatus()
                                                == Status.CANCELLED
                        )
        );
    }

    @Test
    void reservationRelationships_shouldResolveUserAndBook() {
        Reservation reservation =
                new Reservation(
                        cleanCode,
                        user,
                        Instant.now(),
                        null
                );

        entityManager.persist(reservation);
        entityManager.flush();
        entityManager.clear();

        Reservation loaded =
                reservationRepository
                        .findById(
                                reservation.getId()
                        )
                        .orElseThrow();

        assertEquals(
                "repo@example.com",
                loaded.getUser().getEmail()
        );

        assertEquals(
                "Clean Code",
                loaded.getBook().getTitle()
        );
    }
}