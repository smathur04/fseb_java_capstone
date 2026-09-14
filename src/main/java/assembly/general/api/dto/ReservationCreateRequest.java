package assembly.general.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationCreateRequest(
        @NotNull UUID bookId
) {
}