package assembly.general.api.dto;

import assembly.general.api.entity.Role;
import assembly.general.api.entity.Status;

import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResponse (
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Role role,
        Status membershipStatus,
        Instant createdAt,
        String message
) {
}
