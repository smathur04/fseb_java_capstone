package assembly.general.api;

import assembly.general.api.entity.*;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Book cleanCode;
    private Book dune;
    private Book unavailableBook;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        cleanCode =
                bookRepository.save(
                        createBook(
                                "9780132350884",
                                "Clean Code",
                                "Robert C. Martin",
                                "Technology",
                                2008,
                                5,
                                3
                        )
                );

        dune =
                bookRepository.save(
                        createBook(
                                "9780441013593",
                                "Dune",
                                "Frank Herbert",
                                "Science Fiction",
                                1965,
                                4,
                                2
                        )
                );

        unavailableBook =
                bookRepository.save(
                        createBook(
                                "9780000000001",
                                "Unavailable Systems",
                                "Jane Martin",
                                "Technology",
                                2020,
                                2,
                                0
                        )
                );

        bookRepository.flush();
    }

    // =========================================================
    // AUTHENTICATION
    // =========================================================

    @Test
    void registration_shouldCreateUserAndHashPassword()
            throws Exception {

        register(
                "new@example.com",
                "StrongPass1!",
                "New",
                "User"
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "new@example.com"
                                )
                );

        User user =
                userRepository
                        .findByEmail(
                                "new@example.com"
                        )
                        .orElseThrow();

        assertNotEquals(
                "StrongPass1!",
                user.getPasswordHash()
        );

        assertTrue(
                passwordEncoder.matches(
                        "StrongPass1!",
                        user.getPasswordHash()
                )
        );
    }

    @Test
    void registration_shouldRejectDuplicateEmail()
            throws Exception {

        createUser(
                "duplicate@example.com",
                "StrongPass1!",
                Role.PATRON
        );

        register(
                "duplicate@example.com",
                "StrongPass1!",
                "Duplicate",
                "User"
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void registration_shouldRejectInvalidPassword()
            throws Exception {

        register(
                "weak@example.com",
                "password",
                "Weak",
                "User"
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturnJwtForValidCredentials()
            throws Exception {

        createUser(
                "login@example.com",
                "StrongPass1!",
                Role.PATRON
        );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "email":"login@example.com",
                                          "password":"StrongPass1!"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                );
    }

    @Test
    void login_shouldRejectBadCredentials()
            throws Exception {

        createUser(
                "login@example.com",
                "StrongPass1!",
                Role.PATRON
        );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "email":"login@example.com",
                                          "password":"WrongPass1!"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // PROFILE / SECURITY
    // =========================================================

    @Test
    void profile_shouldRequireAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/profile")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profile_shouldWorkWithValidJwt()
            throws Exception {

        createUser(
                "profile@example.com",
                "StrongPass1!",
                Role.PATRON
        );

        String token =
                loginAndGetToken(
                        "profile@example.com",
                        "StrongPass1!"
                );

        mockMvc.perform(
                        get("/api/users/profile")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "profile@example.com"
                                )
                );
    }

    // =========================================================
    // CATALOG
    // =========================================================

    @Test
    void catalog_shouldBePublicAndPaginated()
            throws Exception {

        mockMvc.perform(
                        get("/api/catalog/books")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(20)
                );
    }

    @Test
    void catalog_shouldSearchAndCombineFilters()
            throws Exception {

        mockMvc.perform(
                        get("/api/catalog/books")
                                .param(
                                        "query",
                                        "martin"
                                )
                                .param(
                                        "genre",
                                        "Technology"
                                )
                                .param(
                                        "availableOnly",
                                        "true"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.content",
                                hasSize(1)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].title"
                        ).value(
                                "Clean Code"
                        )
                );
    }

    @Test
    void catalog_shouldSortAndCustomPaginate()
            throws Exception {

        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("page", "0")
                                .param("size", "2")
                                .param(
                                        "sortBy",
                                        "publicationYear"
                                )
                                .param(
                                        "sortOrder",
                                        "desc"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.content",
                                hasSize(2)
                        )
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(2)
                );
    }

    @Test
    void bookDetails_shouldReturnBook()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/catalog/books/{id}",
                                cleanCode.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Clean Code"
                                )
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .exists()
                );
    }

    @Test
    void bookDetails_shouldReturn404ForMissingBook()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/catalog/books/{id}",
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // RESERVATION CREATION
    // =========================================================

    @Test
    void reservation_shouldCreateAndDecrementCopies()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        int before =
                cleanCode.getAvailableCopies();

        mockMvc.perform(
                        post("/api/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        bookIdJson(
                                                cleanCode.getId()
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.status")
                                .value("RESERVED")
                )
                .andExpect(
                        jsonPath("$.expiresAt")
                                .exists()
                );

        Book reloaded =
                bookRepository
                        .findById(
                                cleanCode.getId()
                        )
                        .orElseThrow();

        assertEquals(
                before - 1,
                reloaded.getAvailableCopies()
        );
    }

    @Test
    void reservation_shouldRejectUnavailableBook()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post("/api/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        bookIdJson(
                                                unavailableBook.getId()
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "BOOK_UNAVAILABLE"
                                )
                );
    }

    @Test
    void reservation_shouldEnforceFiveReservationLimit()
            throws Exception {

        User patron =
                createUser(
                        "limit@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        for (int i = 0; i < 6; i++) {
            Book book =
                    bookRepository.save(
                            createBook(
                                    "LIMIT-" + i,
                                    "Book " + i,
                                    "Author",
                                    "Testing",
                                    2020 + i,
                                    1,
                                    1
                            )
                    );

            if (i < 5) {
                mockMvc.perform(
                                post(
                                        "/api/reservations"
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + token
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                bookIdJson(
                                                        book.getId()
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        );
            } else {
                mockMvc.perform(
                                post(
                                        "/api/reservations"
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + token
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                bookIdJson(
                                                        book.getId()
                                                )
                                        )
                        )
                        .andExpect(
                                status()
                                        .isBadRequest()
                        )
                        .andExpect(
                                jsonPath("$.error")
                                        .value(
                                                "RESERVATION_LIMIT_EXCEEDED"
                                        )
                        );
            }
        }
    }

    // =========================================================
    // ACTIVE RESERVATIONS
    // =========================================================

    @Test
    void activeReservations_shouldReturnUsersReservations()
            throws Exception {

        User patron =
                createUser(
                        "active@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        Reservation reservation =
                new Reservation(
                        cleanCode,
                        patron,
                        Instant.now(),
                        null
                );

        reservationRepository.saveAndFlush(
                reservation
        );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        get("/api/reservations")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalActive")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.reservations[0].daysUntilExpiry"
                        ).exists()
                );
    }

    // =========================================================
    // CHECKOUT AUTHORIZATION
    // =========================================================

    @Test
    void checkout_shouldAllowLibrarian()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        User librarian =
                createUser(
                        "librarian@example.com",
                        "StrongPass1!",
                        Role.LIBRARIAN
                );

        Reservation reservation =
                reservationRepository
                        .saveAndFlush(
                                new Reservation(
                                        cleanCode,
                                        patron,
                                        Instant.now(),
                                        null
                                )
                        );

        String token =
                loginAndGetToken(
                        librarian.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/checkout",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "notes":"Good condition"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "CHECKED_OUT"
                                )
                )
                .andExpect(
                        jsonPath("$.dueDate")
                                .exists()
                );
    }

    @Test
    void checkout_shouldRejectPatron()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        Reservation reservation =
                reservationRepository
                        .saveAndFlush(
                                new Reservation(
                                        cleanCode,
                                        patron,
                                        Instant.now(),
                                        null
                                )
                        );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/checkout",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content("{}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void checkout_shouldRejectNonReservedStatus()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        User librarian =
                createUser(
                        "librarian@example.com",
                        "StrongPass1!",
                        Role.LIBRARIAN
                );

        Reservation reservation =
                new Reservation(
                        cleanCode,
                        patron,
                        Instant.now(),
                        null
                );

        reservation.checkOut(
                Instant.now(),
                null
        );

        reservation =
                reservationRepository
                        .saveAndFlush(
                                reservation
                        );

        String token =
                loginAndGetToken(
                        librarian.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/checkout",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_STATUS"
                                )
                );
    }

    // =========================================================
    // RETURN
    // =========================================================

    @Test
    void return_shouldAllowLibrarianAndIncrementCopies()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        User librarian =
                createUser(
                        "librarian@example.com",
                        "StrongPass1!",
                        Role.LIBRARIAN
                );

        cleanCode.checkOutBook();
        bookRepository.saveAndFlush(cleanCode);

        Reservation reservation =
                new Reservation(
                        cleanCode,
                        patron,
                        Instant.now(),
                        null
                );

        reservation.checkOut(
                Instant.now(),
                null
        );

        reservation =
                reservationRepository
                        .saveAndFlush(
                                reservation
                        );

        int before =
                bookRepository
                        .findById(
                                cleanCode.getId()
                        )
                        .orElseThrow()
                        .getAvailableCopies();

        String token =
                loginAndGetToken(
                        librarian.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/return",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "condition":"GOOD",
                                          "notes":"Returned clean"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk());

        Book reloaded =
                bookRepository
                        .findById(
                                cleanCode.getId()
                        )
                        .orElseThrow();

        assertEquals(
                before + 1,
                reloaded.getAvailableCopies()
        );
    }

    @Test
    void return_shouldCalculateLateFee()
            throws Exception {

        User patron =
                createUser(
                        "late@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        User librarian =
                createUser(
                        "librarian@example.com",
                        "StrongPass1!",
                        Role.LIBRARIAN
                );

        Reservation reservation =
                new Reservation(
                        cleanCode,
                        patron,
                        Instant.now()
                                .minus(
                                        Duration.ofDays(18)
                                ),
                        null
                );

        reservation.checkOut(
                Instant.now()
                        .minus(
                                Duration.ofDays(17)
                        ),
                null
        );

        reservation =
                reservationRepository
                        .saveAndFlush(
                                reservation
                        );

        String token =
                loginAndGetToken(
                        librarian.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/return",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "condition":"FAIR"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.lateDays")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.lateFee")
                                .value(3.0)
                );
    }

    @Test
    void return_shouldRejectPatron()
            throws Exception {

        User patron =
                createUser(
                        "returnpatron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        Reservation reservation =
                new Reservation(
                        cleanCode,
                        patron,
                        Instant.now(),
                        null
                );

        reservation.checkOut(
                Instant.now(),
                null
        );

        reservation =
                reservationRepository
                        .saveAndFlush(
                                reservation
                        );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/return",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "condition":"GOOD"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void return_shouldRejectReservedReservation()
            throws Exception {

        User patron =
                createUser(
                        "patron@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        User librarian =
                createUser(
                        "librarian@example.com",
                        "StrongPass1!",
                        Role.LIBRARIAN
                );

        Reservation reservation =
                reservationRepository
                        .saveAndFlush(
                                new Reservation(
                                        cleanCode,
                                        patron,
                                        Instant.now(),
                                        null
                                )
                        );

        String token =
                loginAndGetToken(
                        librarian.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/return",
                                reservation.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "condition":"GOOD"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "INVALID_STATUS"
                                )
                );
    }

    // =========================================================
    // HISTORY
    // =========================================================

    @Test
    void history_shouldBePaginatedAndIncludeWasLate()
            throws Exception {

        User patron =
                createUser(
                        "history@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        Reservation lateReservation =
                new Reservation(
                        cleanCode,
                        patron,
                        Instant.now()
                                .minus(
                                        Duration.ofDays(20)
                                ),
                        null
                );

        lateReservation.checkOut(
                Instant.now()
                        .minus(
                                Duration.ofDays(18)
                        ),
                null
        );

        lateReservation.returnBook(
                Instant.now(),
                Condition.GOOD,
                null
        );

        reservationRepository.saveAndFlush(
                lateReservation
        );

        String token =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        mockMvc.perform(
                        get(
                                "/api/reservations/history"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content")
                                .isArray()
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].wasLate"
                        ).value(true)
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(0)
                );
    }

    // =========================================================
    // FULL WORKFLOW
    // =========================================================

    @Test
    void completeReservationLifecycle_shouldWork()
            throws Exception {

        User patron =
                createUser(
                        "workflow@example.com",
                        "StrongPass1!",
                        Role.PATRON
                );

        User librarian =
                createUser(
                        "workflowlib@example.com",
                        "StrongPass1!",
                        Role.LIBRARIAN
                );

        String patronToken =
                loginAndGetToken(
                        patron.getEmail(),
                        "StrongPass1!"
                );

        String librarianToken =
                loginAndGetToken(
                        librarian.getEmail(),
                        "StrongPass1!"
                );

        String reserveResponse =
                mockMvc.perform(
                                post(
                                        "/api/reservations"
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + patronToken
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                bookIdJson(
                                                        dune.getId()
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID reservationId =
                UUID.fromString(
                        objectMapper
                                .readTree(
                                        reserveResponse
                                )
                                .get(
                                        "reservationId"
                                )
                                .asText()
                );

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/checkout",
                                reservationId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + librarianToken
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content("{}")
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(
                                "/api/reservations/{id}/return",
                                reservationId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + librarianToken
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "condition":"GOOD"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk());

        Reservation finished =
                reservationRepository
                        .findById(
                                reservationId
                        )
                        .orElseThrow();

        assertEquals(
                Status.RETURNED,
                finished.getStatus()
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Book createBook(
            String isbn,
            String title,
            String author,
            String genre,
            int publicationYear,
            int totalCopies,
            int availableCopies
    ) {
        return new Book(
                isbn,
                title,
                author,
                genre,
                publicationYear,
                "Description",
                "Publisher",
                300,
                "English",
                totalCopies,
                availableCopies
        );
    }

    private User createUser(
            String email,
            String password,
            Role role
    ) {
        User user =
                new User(
                        email,
                        passwordEncoder.encode(
                                password
                        ),
                        "Test",
                        "User",
                        "9255551234"
                );

        user.setRole(role);

        return userRepository
                .saveAndFlush(user);
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String email,
            String password,
            String firstName,
            String lastName
    ) throws Exception {

        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                email,
                                "password",
                                password,
                                "firstName",
                                firstName,
                                "lastName",
                                lastName,
                                "phoneNumber",
                                "9255551234"
                        )
                );

        return mockMvc.perform(
                post("/api/auth/register")
                        .contentType(
                                "application/json"
                        )
                        .content(body)
        );
    }

    private String loginAndGetToken(
            String email,
            String password
    ) throws Exception {

        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                email,
                                "password",
                                password
                        )
                );

        String response =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(body)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json =
                objectMapper.readTree(
                        response
                );

        return json
                .get("accessToken")
                .asText();
    }

    private String bookIdJson(
            UUID bookId
    ) throws Exception {

        return objectMapper
                .writeValueAsString(
                        Map.of(
                                "bookId",
                                bookId
                        )
                );
    }
}