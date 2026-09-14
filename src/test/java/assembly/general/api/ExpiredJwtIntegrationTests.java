package assembly.general.api;

import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.repository.UserRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "jwt-expiration-ms=1"
        }
)
@AutoConfigureMockMvc
class ExpiredJwtIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void expiredJwt_shouldNotAccessProtectedEndpoint()
            throws Exception {

        User user =
                new User(
                        "expired@example.com",
                        passwordEncoder.encode(
                                "StrongPass1!"
                        ),
                        "Expired",
                        "Token",
                        "9255551234"
                );

        user.setRole(Role.PATRON);

        userRepository.saveAndFlush(user);

        String response =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                """
                                                {
                                                  "email":"expired@example.com",
                                                  "password":"StrongPass1!"
                                                }
                                                """
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json =
                objectMapper.readTree(
                        response
                );

        String token =
                json.get("accessToken")
                        .asText();

        Thread.sleep(25);

        mockMvc.perform(
                        get("/api/users/profile")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isForbidden());
    }
}