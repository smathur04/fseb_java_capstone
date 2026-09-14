package assembly.general.api;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.Condition;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.Status;
import assembly.general.api.entity.User;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReservationEntityTests {

    private User createUser() {
        return new User(
                "test@example.com",
                "hashed-password",
                "Test",
                "User",
                "9255551234"
        );
    }

    private Book createBook() {
        return new Book(
                "9781234567890",
                "Test Book",
                "Test Author",
                "Technology",
                2020,
                "Test description",
                "Test Publisher",
                300,
                "English",
                5,
                5
        );
    }

    @Test
    void newReservation_shouldStartReservedAndExpireInSevenDays() {
        Instant reservedAt =
                Instant.parse("2026-09-01T12:00:00Z");

        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        reservedAt,
                        null
                );

        assertEquals(Status.RESERVED, reservation.getStatus());
        assertEquals(reservedAt, reservation.getReservedAt());

        assertEquals(
                reservedAt.plus(Duration.ofDays(7)),
                reservation.getExpiresAt()
        );
    }

    @Test
    void checkout_shouldSetCheckedOutStatusAndFourteenDayDueDate() {
        Instant reservedAt =
                Instant.parse("2026-09-01T12:00:00Z");

        Instant checkedOutAt =
                Instant.parse("2026-09-02T12:00:00Z");

        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        reservedAt,
                        null
                );

        reservation.checkOut(
                checkedOutAt,
                "Book looked good"
        );

        assertEquals(
                Status.CHECKED_OUT,
                reservation.getStatus()
        );

        assertEquals(
                checkedOutAt,
                reservation.getCheckedOutAt()
        );

        assertEquals(
                checkedOutAt.plus(Duration.ofDays(14)),
                reservation.getDueDateAt()
        );
    }

    @Test
    void returnBook_shouldReturnWithoutFeeWhenOnTime() {
        Instant reservedAt =
                Instant.parse("2026-09-01T00:00:00Z");

        Instant checkedOutAt =
                Instant.parse("2026-09-02T00:00:00Z");

        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        reservedAt,
                        null
                );

        reservation.checkOut(
                checkedOutAt,
                null
        );

        Instant dueDate =
                reservation.getDueDateAt();

        reservation.returnBook(
                dueDate.minus(Duration.ofHours(1)),
                Condition.GOOD,
                null
        );

        assertEquals(Status.RETURNED, reservation.getStatus());
        assertEquals(0, reservation.getLateDays());

        assertEquals(
                new BigDecimal("0.00"),
                reservation.getLateFee()
        );

        assertEquals(
                Condition.GOOD,
                reservation.getReturnCondition()
        );
    }

    @Test
    void returnBook_shouldChargeOneDollarPerLateDay() {
        Instant reservedAt =
                Instant.parse("2026-09-01T00:00:00Z");

        Instant checkedOutAt =
                Instant.parse("2026-09-02T00:00:00Z");

        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        reservedAt,
                        null
                );

        reservation.checkOut(
                checkedOutAt,
                null
        );

        Instant returnedAt =
                reservation
                        .getDueDateAt()
                        .plus(Duration.ofDays(3));

        reservation.returnBook(
                returnedAt,
                Condition.FAIR,
                "Minor wear"
        );

        assertEquals(Status.RETURNED, reservation.getStatus());
        assertEquals(3, reservation.getLateDays());

        assertEquals(
                new BigDecimal("3.00"),
                reservation.getLateFee()
        );
    }

    @Test
    void checkout_shouldFailWhenReservationIsNotReserved() {
        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        Instant.now(),
                        null
                );

        reservation.checkOut(
                Instant.now(),
                null
        );

        assertThrows(
                IllegalStateException.class,
                () -> reservation.checkOut(
                        Instant.now(),
                        null
                )
        );
    }

    @Test
    void return_shouldFailWhenReservationIsNotCheckedOut() {
        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        Instant.now(),
                        null
                );

        assertThrows(
                IllegalStateException.class,
                () -> reservation.returnBook(
                        Instant.now(),
                        Condition.GOOD,
                        null
                )
        );
    }

    @Test
    void cancel_shouldOnlyWorkForReservedReservation() {
        Reservation reservation =
                new Reservation(
                        createBook(),
                        createUser(),
                        Instant.now(),
                        null
                );

        reservation.cancel(
                Instant.now(),
                "No longer needed",
                null
        );

        assertEquals(
                Status.CANCELLED,
                reservation.getStatus()
        );

        assertNotNull(
                reservation.getCancelledAt()
        );
    }
}