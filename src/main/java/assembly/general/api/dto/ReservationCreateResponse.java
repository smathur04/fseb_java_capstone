package assembly.general.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationCreateResponse(
        UUID reservationId,
        UUID bookId,
        UUID userId,
        String bookTitle,
        String status,
        Instant reservedAt,
        Instant expiresAt,
        String message
) {
}