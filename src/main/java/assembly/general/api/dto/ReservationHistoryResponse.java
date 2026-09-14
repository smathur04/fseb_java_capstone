package assembly.general.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationHistoryResponse(
        UUID reservationId,
        String bookTitle,
        String bookAuthor,
        Instant reservedAt,
        Instant checkedOutAt,
        Instant returnedAt,
        Instant dueDate,
        String status,
        boolean wasLate
) {
}