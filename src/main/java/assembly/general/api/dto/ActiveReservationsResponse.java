package assembly.general.api.dto;

import java.util.List;

public record ActiveReservationsResponse(
        List<ActiveReservationResponse> reservations,
        int totalActive
) {
}