package assembly.general.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRegistrationRequest (

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format")
    String email,

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
            message = "Password must be at least 8 characters and include uppercase, " +
                    "lowercase, a number, and a special character"
    )
    String password,

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Phone number must be a valid format"
    )
    String phoneNumber,

    @NotBlank(message = "Name is required")
    String name
) {
}

