package assembly.general.api;

import assembly.general.api.dto.ReservationCreateRequest;
import assembly.general.api.dto.ReturnRequest;
import assembly.general.api.entity.Book;
import assembly.general.api.entity.Condition;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.Status;
import assembly.general.api.entity.User;
import assembly.general.api.exception.ReservationBusinessException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import assembly.general.api.service.ReservationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    private ReservationService reservationService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        reservationService =
                new ReservationService(
                        reservationRepository,
                        userRepository,
                        bookRepository
                );

        user = new User(
                "test@example.com",
                "hashed-password",
                "Test",
                "User",
                "9255551234"
        );

        book = new Book(
                "9781234567890",
                "Test Book",
                "Test Author",
                "Technology",
                2020,
                "Description",
                "Publisher",
                300,
                "English",
                5,
                5
        );
    }

    @Test
    void createReservation_shouldRejectUserAtFiveActiveReservations() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(
                reservationRepository
                        .countByUserAndStatusIn(
                                eq(user),
                                anyList()
                        )
        ).thenReturn(5L);

        ReservationBusinessException exception =
                assertThrows(
                        ReservationBusinessException.class,
                        () ->
                                reservationService
                                        .createReservation(
                                                userId,
                                                new ReservationCreateRequest(
                                                        UUID.randomUUID()
                                                )
                                        )
                );

        assertEquals(
                "RESERVATION_LIMIT_EXCEEDED",
                exception.getError()
        );

        verifyNoInteractions(bookRepository);
    }

    @Test
    void createReservation_shouldRejectUnavailableBook() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        Book unavailableBook =
                new Book(
                        "9780000000000",
                        "Unavailable",
                        "Author",
                        "Technology",
                        2020,
                        "Description",
                        "Publisher",
                        200,
                        "English",
                        3,
                        0
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(
                reservationRepository
                        .countByUserAndStatusIn(
                                eq(user),
                                anyList()
                        )
        ).thenReturn(0L);

        when(bookRepository.findById(bookId))
                .thenReturn(
                        Optional.of(unavailableBook)
                );

        ReservationBusinessException exception =
                assertThrows(
                        ReservationBusinessException.class,
                        () ->
                                reservationService
                                        .createReservation(
                                                userId,
                                                new ReservationCreateRequest(
                                                        bookId
                                                )
                                        )
                );

        assertEquals(
                "BOOK_UNAVAILABLE",
                exception.getError()
        );
    }

    @Test
    void createReservation_shouldDecrementAvailableCopies() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(
                reservationRepository
                        .countByUserAndStatusIn(
                                eq(user),
                                anyList()
                        )
        ).thenReturn(0L);

        when(bookRepository.findById(bookId))
                .thenReturn(Optional.of(book));

        when(
                reservationRepository.save(
                        any(Reservation.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        assertEquals(
                5,
                book.getAvailableCopies()
        );

        reservationService.createReservation(
                userId,
                new ReservationCreateRequest(bookId)
        );

        assertEquals(
                4,
                book.getAvailableCopies()
        );

        verify(bookRepository).save(book);
    }

    @Test
    void checkout_shouldRejectNonReservedReservation() {
        UUID reservationId =
                UUID.randomUUID();

        Reservation reservation =
                new Reservation(
                        book,
                        user,
                        Instant.now(),
                        null
                );

        reservation.checkOut(
                Instant.now(),
                null
        );

        when(
                reservationRepository
                        .findById(reservationId)
        ).thenReturn(Optional.of(reservation));

        ReservationBusinessException exception =
                assertThrows(
                        ReservationBusinessException.class,
                        () ->
                                reservationService.checkout(
                                        reservationId,
                                        new assembly.general.api.dto.CheckoutRequest(
                                                null
                                        )
                                )
                );

        assertEquals(
                "INVALID_STATUS",
                exception.getError()
        );
    }

    @Test
    void return_shouldRejectReservationThatIsStillReserved() {
        UUID reservationId =
                UUID.randomUUID();

        Reservation reservation =
                new Reservation(
                        book,
                        user,
                        Instant.now(),
                        null
                );

        when(
                reservationRepository
                        .findById(reservationId)
        ).thenReturn(Optional.of(reservation));

        ReservationBusinessException exception =
                assertThrows(
                        ReservationBusinessException.class,
                        () ->
                                reservationService.returnBook(
                                        reservationId,
                                        new ReturnRequest(
                                                Condition.GOOD,
                                                null
                                        )
                                )
                );

        assertEquals(
                "INVALID_STATUS",
                exception.getError()
        );
    }
}