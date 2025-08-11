package org.example.axelnyman.main.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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

import java.time.LocalDateTime;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class AuthIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

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
    void shouldRegisterNewUser() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "John", "Doe", "john.doe@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.firstName", is("John")))
                .andExpect(jsonPath("$.user.lastName", is("Doe")))
                .andExpect(jsonPath("$.user.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.householdId", notNullValue()))
                .andExpect(jsonPath("$.user.createdAt", notNullValue()));

        // Verify user was saved in database
        assertTrue(userRepository.existsByEmail("john.doe@example.com"));

        // Verify password was hashed
        User savedUser = userRepository.findAll().get(0);
        assertThat(savedUser.getHashedPassword(), not(equalTo("password123")));
        assertTrue(passwordEncoder.matches("password123", savedUser.getHashedPassword()));

        // Verify household was created
        assertThat(householdRepository.count(), is(1L));
        var household = householdRepository.findById(savedUser.getHousehold().getId()).orElseThrow();
        assertThat(household.getName(), is("John Doe's Household"));
    }

    @Test
    void shouldValidateJwtTokenClaimsAfterRegistration() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Alice", "Johnson", "alice.johnson@example.com", "password123");

        String responseContent = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token and validate claims
        var responseMap = objectMapper.readValue(responseContent, Map.class);
        String token = (String) responseMap.get("token");
        
        assertTrue(jwtTokenProvider.validateToken(token));
        
        // Verify user was saved and get their details for validation
        User savedUser = userRepository.findAll().get(0);
        
        assertEquals(savedUser.getId().toString(), jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(savedUser.getHousehold().getId().toString(), jwtTokenProvider.getHouseholdIdFromToken(token));
        assertEquals("alice.johnson@example.com", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void shouldAllowImmediateUseOfTokenAfterRegistration() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Bob", "Smith", "bob.smith@example.com", "password123");

        String responseContent = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token from registration response
        var responseMap = objectMapper.readValue(responseContent, Map.class);
        String token = (String) responseMap.get("token");
        
        // Verify the token can be used immediately for protected endpoints (if we had any)
        // For now, just verify the token is valid and has correct claims
        assertTrue(jwtTokenProvider.validateToken(token));
        
        User savedUser = userRepository.findAll().get(0);
        assertEquals(savedUser.getId().toString(), jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(savedUser.getEmail(), jwtTokenProvider.getEmailFromToken(token));
        assertEquals(savedUser.getHousehold().getId().toString(), jwtTokenProvider.getHouseholdIdFromToken(token));
    }

    @Test
    void shouldNotRegisterUserWithDuplicateEmail() throws Exception {
        // Create existing user
        Household household = new Household("Existing Household");
        Household savedHousehold = householdRepository.save(household);
        
        User existingUser = new User("Jane", "Doe", "jane.doe@example.com", "hashedPassword", savedHousehold);
        userRepository.save(existingUser);

        RegisterUserRequest request = new RegisterUserRequest(
                "John", "Doe", "jane.doe@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("already exists")))
                .andExpect(jsonPath("$.details.email[0]", is("Email already exists")));
    }

    @Test
    void shouldNotRegisterUserWithInvalidEmail() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "John", "Doe", "invalid-email", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotRegisterUserWithShortPassword() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "John", "Doe", "john.doe@example.com", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotRegisterUserWithMissingFields() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "", "", "", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Login Tests

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // Create test user
        Household household = new Household("Test Household");
        Household savedHousehold = householdRepository.save(household);
        
        String rawPassword = "password123";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User("John", "Doe", "john.doe@example.com", hashedPassword, savedHousehold);
        User savedUser = userRepository.save(user);

        LoginDto loginDto = new LoginDto("john.doe@example.com", rawPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.id", is(savedUser.getId().intValue())))
                .andExpect(jsonPath("$.user.firstName", is("John")))
                .andExpect(jsonPath("$.user.lastName", is("Doe")))
                .andExpect(jsonPath("$.user.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.user.householdId", is(savedHousehold.getId().intValue())))
                .andExpect(jsonPath("$.user.createdAt", notNullValue()));
    }

    @Test
    void shouldValidateJwtTokenClaimsAfterLogin() throws Exception {
        // Create test user
        Household household = new Household("JWT Test Household");
        Household savedHousehold = householdRepository.save(household);
        
        String rawPassword = "password123";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User("Jane", "Smith", "jane.smith@example.com", hashedPassword, savedHousehold);
        User savedUser = userRepository.save(user);

        LoginDto loginDto = new LoginDto("jane.smith@example.com", rawPassword);

        String responseContent = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token and validate claims
        var responseMap = objectMapper.readValue(responseContent, Map.class);
        String token = (String) responseMap.get("token");
        
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(savedUser.getId().toString(), jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(savedHousehold.getId().toString(), jwtTokenProvider.getHouseholdIdFromToken(token));
        assertEquals("jane.smith@example.com", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidEmail() throws Exception {
        LoginDto loginDto = new LoginDto("nonexistent@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid credentials")));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidPassword() throws Exception {
        // Create test user
        Household household = new Household("Password Test Household");
        Household savedHousehold = householdRepository.save(household);
        
        String correctPassword = "correctPassword123";
        String hashedPassword = passwordEncoder.encode(correctPassword);
        User user = new User("Bob", "Johnson", "bob.johnson@example.com", hashedPassword, savedHousehold);
        userRepository.save(user);

        LoginDto loginDto = new LoginDto("bob.johnson@example.com", "wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid credentials")));
    }

    @Test
    void shouldNotLoginSoftDeletedUser() throws Exception {
        // Create test user
        Household household = new Household("Deleted User Household");
        Household savedHousehold = householdRepository.save(household);
        
        String rawPassword = "password123";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User("Alice", "Williams", "alice.williams@example.com", hashedPassword, savedHousehold);
        user.setDeletedAt(LocalDateTime.now()); // Soft delete the user
        userRepository.save(user);

        LoginDto loginDto = new LoginDto("alice.williams@example.com", rawPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid credentials")));
    }

    @Test
    void shouldReturnBadRequestForInvalidEmailFormat() throws Exception {
        LoginDto loginDto = new LoginDto("invalid-email", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForMissingFields() throws Exception {
        LoginDto loginDto = new LoginDto("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest());
    }
}