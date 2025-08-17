package org.example.axelnyman.main.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.HouseholdInvitation;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.domain.extensions.HouseholdExtensions;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdInvitationRepository;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class HouseholdInvitationIntegrationTest {

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
    private HouseholdInvitationRepository householdInvitationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        householdInvitationRepository.deleteAll();
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
    void shouldCreateInvitationSuccessfully() throws Exception {
        // Create inviting user in household
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");
        User inviter = userRepository.findActiveByEmail("inviter@example.com").orElseThrow();
        
        // Create user to be invited (not in any household)
        createUserWithoutHousehold("invited@example.com", "Jane", "Smith");

        // Create invitation
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "invited@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.householdId", is(inviter.getHousehold().getId().intValue())))
                .andExpect(jsonPath("$.householdName", is(inviter.getHousehold().getName())))
                .andExpect(jsonPath("$.invitedEmail", is("invited@example.com")))
                .andExpect(jsonPath("$.invitedBy.id", is(inviter.getId().intValue())))
                .andExpect(jsonPath("$.invitedBy.firstName", is("John")))
                .andExpect(jsonPath("$.invitedBy.lastName", is("Doe")))
                .andExpect(jsonPath("$.invitedBy.email", is("inviter@example.com")))
                .andExpect(jsonPath("$.invitedBy.joinedAt", notNullValue()))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")));

        // Verify invitation was saved in database
        assertEquals(1, householdInvitationRepository.count());
        HouseholdInvitation savedInvitation = householdInvitationRepository.findAll().get(0);
        assertEquals("invited@example.com", savedInvitation.getInvitedUser().getEmail());
        assertEquals(HouseholdInvitation.InvitationStatus.PENDING, savedInvitation.getStatus());
        assertTrue(savedInvitation.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
    }

    @Test
    void shouldReturn404WhenUserWithEmailNotFound() throws Exception {
        // Create inviting user
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");

        // Try to invite non-existent user
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "nonexistent@example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("User with this email not found")));

        // Verify no invitation was created
        assertEquals(0, householdInvitationRepository.count());
    }

    @Test
    void shouldReturn400WhenUserAlreadyInSameHousehold() throws Exception {
        // Create household with two users
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");
        User inviter = userRepository.findActiveByEmail("inviter@example.com").orElseThrow();
        Household household = inviter.getHousehold();

        // Add second user to same household
        User existingMember = new User("Jane", "Smith", "existing@example.com", "password123", household);
        userRepository.save(existingMember);

        // Try to invite user who is already in the household
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "existing@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("User already belongs to your household")));

        // Verify no invitation was created
        assertEquals(0, householdInvitationRepository.count());
    }

    @Test
    void shouldReturn400WhenActiveInvitationAlreadyExists() throws Exception {
        // Create inviting user and target user
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");
        User inviter = userRepository.findActiveByEmail("inviter@example.com").orElseThrow();
        createUserWithoutHousehold("invited@example.com", "Jane", "Smith");
        User invited = userRepository.findActiveByEmail("invited@example.com").orElseThrow();

        // Create existing active invitation
        HouseholdInvitation existingInvitation = HouseholdExtensions.toInvitationEntity(
                inviter.getHousehold(), invited, inviter);
        householdInvitationRepository.save(existingInvitation);

        // Try to create another invitation for the same user
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "invited@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Active invitation already exists for this user")));

        // Verify only one invitation exists
        assertEquals(1, householdInvitationRepository.count());
    }

    @Test
    void shouldReturn400WhenEmailIsEmpty() throws Exception {
        // Create inviting user
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");

        // Try to create invitation with empty email
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid email")))
                .andExpect(jsonPath("$.details.email[0]", is("Email is required")));

        // Verify no invitation was created
        assertEquals(0, householdInvitationRepository.count());
    }

    @Test
    void shouldReturn400WhenEmailFormatIsInvalid() throws Exception {
        // Create inviting user
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");

        // Try to create invitation with invalid email format
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "invalid-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid email")))
                .andExpect(jsonPath("$.details.email[0]", is("Email should be valid")));

        // Verify no invitation was created
        assertEquals(0, householdInvitationRepository.count());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // Try to create invitation without authentication
        mockMvc.perform(post("/api/households/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGenerateUniqueTokensForMultipleInvitations() throws Exception {
        // Create two different households with users
        String inviter1Token = createUserAndGetToken("inviter1@example.com", "John", "Doe");
        String inviter2Token = createUserAndGetToken("inviter2@example.com", "Bob", "Johnson");
        
        // Create users to be invited
        createUserWithoutHousehold("invited1@example.com", "Jane", "Smith");
        createUserWithoutHousehold("invited2@example.com", "Alice", "Brown");

        // Create first invitation
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviter1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "invited1@example.com"))))
                .andExpect(status().isCreated());

        // Create second invitation
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviter2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "invited2@example.com"))))
                .andExpect(status().isCreated());

        // Verify both invitations have unique tokens
        assertEquals(2, householdInvitationRepository.count());
        var invitations = householdInvitationRepository.findAll();
        String token1 = invitations.get(0).getToken();
        String token2 = invitations.get(1).getToken();
        
        assertTrue(!token1.equals(token2), "Invitation tokens should be unique");
    }

    @Test
    void shouldAllowInvitationToUserFromDifferentHousehold() throws Exception {
        // Create user in first household
        String inviterToken = createUserAndGetToken("inviter@example.com", "John", "Doe");
        
        // Create user in second household
        createUserAndGetToken("other@example.com", "Jane", "Smith");

        // Inviter should be able to invite user from different household
        mockMvc.perform(post("/api/households/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + inviterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "other@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitedEmail", is("other@example.com")));

        // Verify invitation was created
        assertEquals(1, householdInvitationRepository.count());
    }

    private String createUserAndGetToken(String email, String firstName, String lastName) throws Exception {
        // Create household first
        Household household = HouseholdExtensions.toEntity("Test Household");
        householdRepository.save(household);

        // Register user
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "email", email,
                        "password", "password123"))))
                .andExpect(status().isCreated());

        // Login to get token
        var loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        var responseContent = objectMapper.readTree(loginResponse.getResponse().getContentAsString());
        return responseContent.get("token").asText();
    }

    private void createUserWithoutHousehold(String email, String firstName, String lastName) throws Exception {
        // Register user without associating with a household initially
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "email", email,
                        "password", "password123"))))
                .andExpect(status().isCreated());
        
        // Remove household association to simulate user not in any household
        User user = userRepository.findActiveByEmail(email).orElseThrow();
        user.setHousehold(null);
        userRepository.save(user);
    }
}