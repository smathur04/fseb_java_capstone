package assembly.general.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckoutResponse(
        UUID reservationId,
        String status,
        Instant checkedOutAt,
        Instant dueDate,
        String message
) {
}