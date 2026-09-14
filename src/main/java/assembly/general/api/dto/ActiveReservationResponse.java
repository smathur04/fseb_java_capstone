package assembly.general.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActiveReservationResponse(
        UUID reservationId,
        UUID bookId,
        String bookTitle,
        String bookAuthor,
        String status,

        Instant reservedAt,
        Instant expiresAt,
        Integer daysUntilExpiry,

        Instant checkedOutAt,
        Instant dueDate,
        Integer daysUntilDue
) {
}