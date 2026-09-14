package assembly.general.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReturnResponse(
        UUID reservationId,
        Instant returnedAt,
        Instant dueDate,
        Integer lateDays,
        BigDecimal lateFee,
        String message
) {
}