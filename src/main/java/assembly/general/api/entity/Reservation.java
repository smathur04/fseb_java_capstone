package assembly.general.api.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "reservations")
public class Reservation {

    private static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("1.00");
    private static final Duration RESERVATION_EXPIRY = Duration.ofDays(7);
    private static final Duration CHECK_OUT_EXPIRY = Duration.ofDays(14);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private Instant reservedAt;

    @Column
    private Instant expiresAt;

    @Column
    private Instant checkedOutAt;

    @Column
    private Instant dueDateAt;

    @Column
    private Instant returnedAt;

    @Column
    private Instant cancelledAt;

    @Column
    private Integer renewalCount;

    @Column
    private Integer lateDays;

    @Column
    private BigDecimal lateFee;

    @Column
    private String returnCondition;

    @Column
    private String notes;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Reservation() {
    }

    public Reservation(Book book, User user, Instant reservedAt, String notes) {
        this.book = book;
        this.user = user;
        this.status = Status.RESERVED;
        this.reservedAt = reservedAt;
        this.expiresAt = reservedAt.plus(RESERVATION_EXPIRY);
        appendNote(withCallerNote("Reserved on " + reservedAt, notes));
    }

    public void checkOut(Instant checkedOutAt, String notes) {
        if (status != Status.RESERVED) {
            throw new IllegalStateException("Cannot check out a reservation with status " + status);
        }
        this.status = Status.CHECKED_OUT;
        this.checkedOutAt = checkedOutAt;
        this.dueDateAt = checkedOutAt.plus(CHECK_OUT_EXPIRY);
        this.renewalCount = 0;
        this.lateDays = 0;
        this.lateFee = new BigDecimal("0.00");
        appendNote(withCallerNote("Checked out on " + checkedOutAt, notes));
    }

    public void returnBook(Instant returnedAt, String returnCondition, String notes) {
        if (status != Status.CHECKED_OUT) {
            throw new IllegalStateException("Cannot return a reservation with status " + status);
        }
        this.returnedAt = returnedAt;
        this.returnCondition = returnCondition;
        this.lateDays = calculateLateDays();
        this.lateFee = calculateLateFee();

        String lateNote = lateDays > 0
                ? " - " + lateDays + " day(s) late, fee: $" + lateFee
                : "";
        appendNote(withCallerNote("Returned on " + returnedAt + lateNote, notes));

        this.status = Status.RETURNED;
    }

    public void cancel(Instant cancelledAt, String cancellationReason, String notes) {
        if (status != Status.RESERVED) {
            throw new IllegalStateException("Cannot cancel a reservation with status " + status);
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = cancelledAt;
        String reasonPart = (cancellationReason == null || cancellationReason.isBlank())
                ? ""
                : ": " + cancellationReason;
        appendNote(withCallerNote("Cancelled on " + cancelledAt + reasonPart, notes));
    }

    private String withCallerNote(String systemMessage, String callerNote) {
        if (callerNote == null || callerNote.isBlank()) {
            return systemMessage;
        }
        return systemMessage + "\n" + callerNote;
    }

    private void appendNote(String note) {
        this.notes = (notes == null || notes.isBlank()) ? note : notes + "\n\n" + note;
    }

    public UUID getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public Instant getDueDateAt() {
        return dueDateAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Integer getRenewalCount() {
        return renewalCount;
    }

    public Integer getLateDays() {
        return lateDays;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public String getReturnCondition() {
        return returnCondition;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public Integer calculateLateDays() {
        if (dueDateAt == null) {
            return 0;
        }
        Instant compareTo = returnedAt != null ? returnedAt : Instant.now();
        if (!compareTo.isAfter(dueDateAt)) {
            return 0;
        }
        return (int) Duration.between(dueDateAt, compareTo).toDays();
    }

    public BigDecimal calculateLateFee() {
        int days = calculateLateDays();
        return LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(days));
    }

}