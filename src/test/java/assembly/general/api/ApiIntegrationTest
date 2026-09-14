package assembly.general.api;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.repository.BookRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Book cleanCode;
    private Book dune;
    private Book effectiveJava;
    private Book unavailableBook;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        userRepository.deleteAll();

        cleanCode = new Book(
                "9780132350884",
                "Clean Code",
                "Robert C. Martin",
                "Technology",
                2008,
                "A handbook of agile software craftsmanship.",
                "Prentice Hall",
                464,
                "English",
                5,
                3
        );

        dune = new Book(
                "9780441013593",
                "Dune",
                "Frank Herbert",
                "Science Fiction",
                1965,
                "A science fiction novel.",
                "Ace",
                688,
                "English",
                4,
                2
        );

        effectiveJava = new Book(
                "9780134685991",
                "Effective Java",
                "Joshua Bloch",
                "Technology",
                2018,
                "Best practices for the Java platform.",
                "Addison-Wesley",
                416,
                "English",
                6,
                6
        );

        unavailableBook = new Book(
                "9780000000001",
                "Unavailable Systems",
                "Jane Martin",
                "Technology",
                2020,
                "A book with no available copies.",
                "Example Press",
                300,
                "English",
                2,
                0
        );

        cleanCode = bookRepository.save(cleanCode);
        dune = bookRepository.save(dune);
        effectiveJava = bookRepository.save(effectiveJava);
        unavailableBook = bookRepository.save(unavailableBook);

        bookRepository.flush();
    }

    // =========================================================
    // USER REGISTRATION
    // =========================================================

    @Test
    void register_shouldCreateUser() throws Exception {
        String body = """
                {
                  "email": "shaan@example.com",
                  "password": "StrongPass1!",
                  "firstName": "Shaan",
                  "lastName": "Mathur",
                  "phoneNumber": "+19255551234"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("shaan@example.com"))
                .andExpect(jsonPath("$.firstName").value("Shaan"))
                .andExpect(jsonPath("$.lastName").value("Mathur"))
                .andExpect(jsonPath("$.role").value("PATRON"))
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.message").value("Registration successful."));

        assertTrue(userRepository.existsByEmail("shaan@example.com"));
    }

    @Test
    void register_shouldHashPasswordInsteadOfStoringPlaintext() throws Exception {
        String rawPassword = "StrongPass1!";

        String body = """
                {
                  "email": "hash@example.com",
                  "password": "StrongPass1!",
                  "firstName": "Hash",
                  "lastName": "Test",
                  "phoneNumber": "+19255551234"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail("hash@example.com")
                .orElseThrow();

        assertNotEquals(rawPassword, saved.getPasswordHash());
        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        saved.getPasswordHash()
                )
        );
    }

    @Test
    void register_shouldApplyDefaultRoleAndMembershipStatus() throws Exception {
        String body = """
                {
                  "email": "defaults@example.com",
                  "password": "StrongPass1!",
                  "firstName": "Default",
                  "lastName": "User",
                  "phoneNumber": "9255551234"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail("defaults@example.com")
                .orElseThrow();

        assertEquals(Role.PATRON, saved.getRole());
        assertEquals(
                MembershipStatus.ACTIVE,
                saved.getMembershipStatus()
        );
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getMemberSince());
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {
        User existing = new User(
                "duplicate@example.com",
                passwordEncoder.encode("StrongPass1!"),
                "Existing",
                "User",
                "9255551234"
        );

        userRepository.saveAndFlush(existing);

        String body = """
                {
                  "email": "duplicate@example.com",
                  "password": "StrongPass1!",
                  "firstName": "Other",
                  "lastName": "User",
                  "phoneNumber": "9255559999"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void register_shouldRejectInvalidEmail() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "password": "StrongPass1!",
                  "firstName": "Invalid",
                  "lastName": "Email",
                  "phoneNumber": "9255551234"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void register_shouldRejectWeakPassword() throws Exception {
        String body = """
                {
                  "email": "weak@example.com",
                  "password": "password",
                  "firstName": "Weak",
                  "lastName": "Password",
                  "phoneNumber": "9255551234"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void register_shouldRejectInvalidPhoneNumber() throws Exception {
        String body = """
                {
                  "email": "phone@example.com",
                  "password": "StrongPass1!",
                  "firstName": "Phone",
                  "lastName": "Test",
                  "phoneNumber": "abc"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // =========================================================
    // USER LOGIN
    // =========================================================

    @Test
    void login_shouldReturnJwtForValidCredentials() throws Exception {
        createUser(
                "login@example.com",
                "StrongPass1!",
                "Login",
                "User"
        );

        String body = """
                {
                  "email": "login@example.com",
                  "password": "StrongPass1!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("Login"))
                .andExpect(jsonPath("$.user.lastName").value("User"))
                .andExpect(jsonPath("$.user.role").value("PATRON"));
    }

    @Test
    void login_shouldRejectIncorrectPassword() throws Exception {
        createUser(
                "wrongpassword@example.com",
                "StrongPass1!",
                "Wrong",
                "Password"
        );

        String body = """
                {
                  "email": "wrongpassword@example.com",
                  "password": "WrongPassword1!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void login_shouldRejectUnknownEmail() throws Exception {
        String body = """
                {
                  "email": "missing@example.com",
                  "password": "StrongPass1!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content(body)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }

    // =========================================================
    // PROTECTED USER PROFILE
    // =========================================================

    @Test
    void profile_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/users/profile")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profile_shouldReturnAuthenticatedUser() throws Exception {
        createUser(
                "profile@example.com",
                "StrongPass1!",
                "Profile",
                "User"
        );

        String token = loginAndGetToken(
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
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.firstName").value("Profile"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phoneNumber").value("9255551234"))
                .andExpect(jsonPath("$.role").value("PATRON"))
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.memberSince").exists())
                .andExpect(jsonPath("$.activeReservations").value(0))
                .andExpect(jsonPath("$.borrowingHistory").value(0));
    }

    // =========================================================
    // CATALOG LISTING / DEFAULT PAGINATION
    // =========================================================

    @Test
    void getBooks_shouldReturnAllBooksWithDefaultPagination() throws Exception {
        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getBooks_shouldUseTitleAscendingByDefault() throws Exception {
        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[1].title").value("Dune"))
                .andExpect(jsonPath("$.content[2].title").value("Effective Java"))
                .andExpect(jsonPath("$.content[3].title").value("Unavailable Systems"));
    }

    // =========================================================
    // PAGINATION
    // =========================================================

    @Test
    void getBooks_shouldPaginateResults() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getBooks_shouldReturnSecondPage() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("page", "1")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    // =========================================================
    // SORTING
    // =========================================================

    @Test
    void getBooks_shouldSortByPublicationYearDescending() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("sortBy", "publicationYear")
                                .param("sortOrder", "desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicationYear").value(2020))
                .andExpect(jsonPath("$.content[1].publicationYear").value(2018))
                .andExpect(jsonPath("$.content[2].publicationYear").value(2008))
                .andExpect(jsonPath("$.content[3].publicationYear").value(1965));
    }

    @Test
    void getBooks_shouldSortByAuthorAscending() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("sortBy", "author")
                                .param("sortOrder", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author").value("Frank Herbert"))
                .andExpect(jsonPath("$.content[1].author").value("Jane Martin"))
                .andExpect(jsonPath("$.content[2].author").value("Joshua Bloch"))
                .andExpect(jsonPath("$.content[3].author").value("Robert C. Martin"));
    }

    @Test
    void getBooks_shouldRejectInvalidSortField() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("sortBy", "banana")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getBooks_shouldRejectInvalidSortOrder() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("sortOrder", "sideways")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void getBooks_shouldSearchByTitleCaseInsensitively() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("query", "clean")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    @Test
    void getBooks_shouldSearchByAuthor() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("query", "martin")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getBooks_shouldReturnEmptyPageWhenSearchHasNoMatches() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("query", "does-not-exist")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    // =========================================================
    // FILTERS
    // =========================================================

    @Test
    void getBooks_shouldFilterByExactGenre() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("genre", "Technology")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void getBooks_shouldFilterByExactIsbn() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("isbn", "9780441013593")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Dune"));
    }

    @Test
    void getBooks_shouldFilterAvailableOnly() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("availableOnly", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(
                        jsonPath(
                                "$.content[*].availableCopies",
                                everyItem(greaterThan(0))
                        )
                );
    }

    // =========================================================
    // COMBINED FILTERING
    // =========================================================

    @Test
    void getBooks_shouldCombineQueryGenreAndAvailabilityFilters() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("query", "martin")
                                .param("genre", "Technology")
                                .param("availableOnly", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    // =========================================================
    // DYNAMIC STATUS
    // =========================================================

    @Test
    void getBooks_shouldCalculateAvailableStatus() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("isbn", "9780132350884")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availableCopies").value(3))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    void getBooks_shouldCalculateCheckedOutStatus() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                                .param("isbn", "9780000000001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availableCopies").value(0))
                .andExpect(jsonPath("$.content[0].status").value("CHECKED_OUT"));
    }

    // =========================================================
    // BOOK DETAILS
    // =========================================================

    @Test
    void getBookById_shouldReturnCompleteBookDetails() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/catalog/books/{bookId}",
                                cleanCode.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(cleanCode.getId().toString()))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.genre").value("Technology"))
                .andExpect(jsonPath("$.publicationYear").value(2008))
                .andExpect(jsonPath("$.publisher").value("Prentice Hall"))
                .andExpect(jsonPath("$.pageCount").value(464))
                .andExpect(jsonPath("$.language").value("English"))
                .andExpect(jsonPath("$.totalCopies").value(5))
                .andExpect(jsonPath("$.availableCopies").value(3))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getBookById_shouldReturn404WhenBookDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/catalog/books/{bookId}",
                                missingId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Book not found with ID: "
                                                + missingId
                                )
                )
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // =========================================================
    // SECURITY / PUBLIC ACCESS
    // =========================================================

    @Test
    void catalogListing_shouldBePublic() throws Exception {
        mockMvc.perform(
                        get("/api/catalog/books")
                )
                .andExpect(status().isOk());
    }

    @Test
    void catalogDetails_shouldBePublic() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/catalog/books/{bookId}",
                                dune.getId()
                        )
                )
                .andExpect(status().isOk());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User createUser(
            String email,
            String rawPassword,
            String firstName,
            String lastName
    ) {
        User user = new User(
                email,
                passwordEncoder.encode(rawPassword),
                firstName,
                lastName,
                "9255551234"
        );

        return userRepository.saveAndFlush(user);
    }

    private String loginAndGetToken(
            String email,
            String password
    ) throws Exception {
        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String response =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType("application/json")
                                        .content(requestBody)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        return json.get("accessToken").asText();
    }
}