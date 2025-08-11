package org.example.axelnyman.main.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.example.axelnyman.main.domain.dtos.UserDtos.RegisterUserRequest;
import org.example.axelnyman.main.domain.dtos.UserDtos.LoginDto;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;
import org.example.axelnyman.main.infrastructure.security.JwtTokenProvider;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class JwtAuthenticationFilterIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();
        householdRepository.deleteAll();
    }

    @AfterAll
    static void cleanup() {
        if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
            postgreSQLContainer.stop();
        }
    }

    @Test
    void shouldAccessPublicEndpointsWithoutToken() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "John", "Doe", "john.doe@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginDto loginDto = new LoginDto("john.doe@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401ForProtectedEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication required to access this resource")));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Unauthorized")));

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAccessProtectedEndpointsWithValidToken() throws Exception {
        // Create test user and get token
        String token = createUserAndGetToken("alice@example.com", "Alice", "Smith");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.householdId", notNullValue()))
                .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void shouldReturn401ForMalformedToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "NotBearer token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForExpiredToken() throws Exception {
        // Create a token provider with very short expiration for testing
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
                "testSecretForShortExpirationTesting123456789", 
                1 // 1 millisecond expiration
        );

        // Create test user
        Household household = new Household("Test Household");
        Household savedHousehold = householdRepository.save(household);
        User user = new User("Bob", "Jones", "bob@example.com", "hashedPassword", savedHousehold);
        User savedUser = userRepository.save(user);

        // Generate token that expires immediately
        String expiredToken = shortExpirationProvider.generateToken(
                savedUser.getId(), savedHousehold.getId(), "bob@example.com");

        // Wait for token to expire
        Thread.sleep(10);

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldInjectCurrentUserInControllerMethods() throws Exception {
        String token = createUserAndGetToken("charlie@example.com", "Charlie", "Brown");
        User savedUser = userRepository.findAll().get(0);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", is(savedUser.getId().intValue())))
                .andExpect(jsonPath("$.householdId", is(savedUser.getHousehold().getId().intValue())))
                .andExpect(jsonPath("$.email", is("charlie@example.com")));
    }

    @Test
    void shouldHandleTokenWithValidFormatButNonExistentUser() throws Exception {
        // Create a token with valid format but non-existent user ID
        String tokenWithNonExistentUser = jwtTokenProvider.generateToken(999999L, 999999L, "nonexistent@example.com");

        // The token is valid from JWT perspective, so it should allow access
        // The application logic will determine if the user exists or not
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithNonExistentUser))
                .andExpect(status().isOk()); // JWT is valid, so access is granted
    }

    @Test
    void shouldNotAffectPublicEndpointsWithToken() throws Exception {
        String token = createUserAndGetToken("diana@example.com", "Diana", "Ross");

        RegisterUserRequest request = new RegisterUserRequest(
                "Elvis", "Presley", "elvis@example.com", "password123");

        // Public endpoints should work regardless of token presence
        mockMvc.perform(post("/api/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginDto loginDto = new LoginDto("elvis@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleMultipleRequestsWithSameToken() throws Exception {
        String token = createUserAndGetToken("frank@example.com", "Frank", "Sinatra");

        // Multiple requests with same token should all work
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/users/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("frank@example.com")));
        }
    }

    @Test
    void shouldClearSecurityContextOnInvalidToken() throws Exception {
        String validToken = createUserAndGetToken("grace@example.com", "Grace", "Kelly");
        String invalidToken = "invalid.token.here";

        // First request with valid token should work
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        // Second request with invalid token should fail (security context should be cleared)
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    private String createUserAndGetToken(String email, String firstName, String lastName) throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(firstName, lastName, email, "password123");

        String responseContent = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var responseMap = objectMapper.readValue(responseContent, Map.class);
        return (String) responseMap.get("token");
    }
}