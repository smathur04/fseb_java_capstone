package assembly.general.api.service;

import assembly.general.api.dto.*;
import assembly.general.api.entity.Status;
import assembly.general.api.entity.User;
import assembly.general.api.exception.BadLoginCredException;
import assembly.general.api.exception.DuplicateEmailException;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReservationRepository reservationRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.reservationRepository = reservationRepository;
    }

    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.phoneNumber()
        );

        User saved = userRepository.save(user);

        return new UserRegistrationResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole(),
                saved.getMembershipStatus(),
                saved.getCreatedAt(),
                "Registration successful."
        );
    }

    public UserLoginResponse login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadLoginCredException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadLoginCredException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        UserLoginResponse.UserSummary userSummary = new UserLoginResponse.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );

        return new UserLoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                userSummary
        );
    }

    public UserProfile profile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long activeReservations = reservationRepository.countByUserAndStatusIn(
                user, List.of(Status.RESERVED, Status.CHECKED_OUT));

        long borrowingHistory = reservationRepository.countByUserAndStatus(
                user, Status.RETURNED);

        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getMembershipStatus(),
                user.getMemberSince(),
                (int) activeReservations,
                (int) borrowingHistory
        );
    }

}
