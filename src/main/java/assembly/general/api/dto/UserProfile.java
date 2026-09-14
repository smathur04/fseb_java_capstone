package assembly.general.api.dto;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Role role,
        MembershipStatus membershipStatus,
        Instant memberSince,
        int activeReservations,
        int borrowingHistory
) {
}