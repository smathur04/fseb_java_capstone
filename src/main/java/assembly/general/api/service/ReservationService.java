package assembly.general.api.service;

import assembly.general.api.dto.*;
import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.Status;
import assembly.general.api.entity.User;
import assembly.general.api.exception.ReservationBusinessException;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReservationService {

    private static final int MAX_ACTIVE_RESERVATIONS = 5;

    private static final List<Status> ACTIVE_STATUSES =
            List.of(
                    Status.RESERVED,
                    Status.CHECKED_OUT
            );

    private static final DateTimeFormatter DUE_DATE_FORMAT =
            DateTimeFormatter
                    .ofPattern(
                            "MMMM d, uuuu",
                            Locale.ENGLISH
                    )
                    .withZone(ZoneOffset.UTC);

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            BookRepository bookRepository
    ) {
        this.reservationRepository =
                reservationRepository;

        this.userRepository =
                userRepository;

        this.bookRepository =
                bookRepository;
    }

    @Transactional
    public ReservationCreateResponse createReservation(
            UUID userId,
            ReservationCreateRequest request
    ) {
        User user = getUser(userId);

        long activeCount =
                reservationRepository
                        .countByUserAndStatusIn(
                                user,
                                ACTIVE_STATUSES
                        );

        if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
            throw new ReservationBusinessException(
                    "RESERVATION_LIMIT_EXCEEDED",
                    "You have reached the maximum of 5 active reservations",
                    "currentReservations",
                    activeCount
            );
        }

        Book book = bookRepository
                .findById(request.bookId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Book not found with ID: "
                                        + request.bookId()
                        )
                );

        if (book.getAvailableCopies() <= 0) {
            throw new ReservationBusinessException(
                    "BOOK_UNAVAILABLE",
                    "No copies available for reservation",
                    "availableCopies",
                    book.getAvailableCopies()
            );
        }

        Instant now = Instant.now();

        Reservation reservation =
                new Reservation(
                        book,
                        user,
                        now,
                        null
                );

        book.checkOutBook();

        bookRepository.save(book);

        Reservation saved =
                reservationRepository.save(reservation);

        return new ReservationCreateResponse(
                saved.getId(),
                book.getId(),
                user.getId(),
                book.getTitle(),
                saved.getStatus().name(),
                saved.getReservedAt(),
                saved.getExpiresAt(),
                "Book reserved successfully. Please pick up within 7 days."
        );
    }

    @Transactional(readOnly = true)
    public ActiveReservationsResponse getActiveReservations(
            UUID userId
    ) {
        User user = getUser(userId);

        List<Reservation> reservations =
                reservationRepository
                        .findByUserAndStatusInOrderByReservedAtDesc(
                                user,
                                ACTIVE_STATUSES
                        );

        List<ActiveReservationResponse> responses =
                reservations
                        .stream()
                        .map(this::toActiveResponse)
                        .toList();

        return new ActiveReservationsResponse(
                responses,
                responses.size()
        );
    }

    @Transactional
    public CheckoutResponse checkout(
            UUID reservationId,
            CheckoutRequest request
    ) {
        Reservation reservation =
                getReservation(reservationId);

        if (reservation.getStatus()
                != Status.RESERVED) {

            throw new ReservationBusinessException(
                    "INVALID_STATUS",
                    "Can only checkout reservations with RESERVED status",
                    "currentStatus",
                    reservation.getStatus().name()
            );
        }

        Instant now = Instant.now();

        reservation.checkOut(
                now,
                request.notes()
        );

        Reservation saved =
                reservationRepository.save(reservation);

        String formattedDueDate =
                DUE_DATE_FORMAT.format(
                        saved.getDueDateAt()
                );

        return new CheckoutResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getCheckedOutAt(),
                saved.getDueDateAt(),
                "Book checked out successfully. Due date: "
                        + formattedDueDate
        );
    }

    @Transactional
    public ReturnResponse returnBook(
            UUID reservationId,
            ReturnRequest request
    ) {
        Reservation reservation =
                getReservation(reservationId);

        if (reservation.getStatus()
                != Status.CHECKED_OUT) {

            throw new ReservationBusinessException(
                    "INVALID_STATUS",
                    "Can only return books with CHECKED_OUT status",
                    "currentStatus",
                    reservation.getStatus().name()
            );
        }

        Instant now = Instant.now();

        reservation.returnBook(
                now,
                request.condition(),
                request.notes()
        );

        Book book = reservation.getBook();

        book.returnBook();

        bookRepository.save(book);

        Reservation saved =
                reservationRepository.save(reservation);

        BigDecimal lateFee =
                saved.getLateFee();

        String message =
                lateFee != null
                        && lateFee.compareTo(BigDecimal.ZERO) > 0
                        ? "Book returned. Late fee of $"
                        + lateFee.setScale(2)
                        + " applied to account."
                        : "Book returned successfully";

        return new ReturnResponse(
                saved.getId(),
                saved.getReturnedAt(),
                saved.getDueDateAt(),
                saved.getLateDays(),
                saved.getLateFee(),
                message
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationHistoryResponse> getHistory(
            UUID userId,
            int page,
            int size
    ) {
        validatePagination(page, size);

        User user = getUser(userId);

        Page<Reservation> history =
                reservationRepository
                        .findHistoryByUser(
                                user,
                                PageRequest.of(
                                        page,
                                        size
                                )
                        );

        List<ReservationHistoryResponse> content =
                history.getContent()
                        .stream()
                        .map(this::toHistoryResponse)
                        .toList();

        return new PageResponse<>(
                content,
                history.getNumber(),
                history.getSize(),
                history.getTotalElements(),
                history.getTotalPages(),
                history.isLast()
        );
    }

    private ActiveReservationResponse toActiveResponse(
            Reservation reservation
    ) {
        Integer daysUntilExpiry = null;
        Integer daysUntilDue = null;

        if (reservation.getStatus()
                == Status.RESERVED) {

            daysUntilExpiry =
                    daysUntil(
                            reservation.getExpiresAt()
                    );
        }

        if (reservation.getStatus()
                == Status.CHECKED_OUT) {

            daysUntilDue =
                    daysUntil(
                            reservation.getDueDateAt()
                    );
        }

        return new ActiveReservationResponse(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getBook().getAuthor(),
                reservation.getStatus().name(),

                reservation.getReservedAt(),
                reservation.getExpiresAt(),
                daysUntilExpiry,

                reservation.getCheckedOutAt(),
                reservation.getDueDateAt(),
                daysUntilDue
        );
    }

    private ReservationHistoryResponse toHistoryResponse(
            Reservation reservation
    ) {
        boolean wasLate =
                reservation.getReturnedAt() != null
                        && reservation.getDueDateAt() != null
                        && reservation
                        .getReturnedAt()
                        .isAfter(
                                reservation.getDueDateAt()
                        );

        return new ReservationHistoryResponse(
                reservation.getId(),
                reservation.getBook().getTitle(),
                reservation.getBook().getAuthor(),
                reservation.getReservedAt(),
                reservation.getCheckedOutAt(),
                reservation.getReturnedAt(),
                reservation.getDueDateAt(),
                reservation.getStatus().name(),
                wasLate
        );
    }

    private int daysUntil(Instant target) {
        if (target == null) {
            return 0;
        }

        long seconds =
                Duration.between(
                        Instant.now(),
                        target
                ).getSeconds();

        if (seconds <= 0) {
            return 0;
        }

        return (int) Math.ceil(
                seconds / 86400.0
        );
    }

    private User getUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User not found with ID: "
                                        + userId
                        )
                );
    }

    private Reservation getReservation(
            UUID reservationId
    ) {
        return reservationRepository
                .findById(reservationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Reservation not found with ID: "
                                        + reservationId
                        )
                );
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Page size must be greater than zero"
            );
        }
    }
}