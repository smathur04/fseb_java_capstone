package assembly.general.api.controllers;

import assembly.general.api.dto.*;
import assembly.general.api.security.AuthenticatedUser;
import assembly.general.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserRegistrationResponse> authRegister(
            @Valid @RequestBody UserRegistrationRequest request
    )
    {
        UserRegistrationResponse created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserLoginResponse> authLogin(
            @Valid @RequestBody UserLoginRequest request
    )
    {
        UserLoginResponse created = userService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(created);
    }

    @GetMapping("/users/profile")
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        UserProfile profile = userService.profile(principal.userId());
        return ResponseEntity.ok(profile);
    }
}