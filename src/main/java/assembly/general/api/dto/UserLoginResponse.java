package assembly.general.api.dto;

import assembly.general.api.entity.Role;

import java.util.UUID;

public record UserLoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserSummary user
) {
    public record UserSummary(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            Role role
    ) {
    }
}