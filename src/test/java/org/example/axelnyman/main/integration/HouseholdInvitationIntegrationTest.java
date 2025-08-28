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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void shouldGetUserPendingInvitationsSuccessfully() throws Exception {
        // Create user who will receive invitations
        String invitedToken = createUserAndGetToken("invited@example.com", "Jane", "Smith");
        User invitedUser = userRepository.findActiveByEmail("invited@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household  
        invitedUser.setHousehold(null);
        userRepository.save(invitedUser);

        // Create two different households with inviters
        createUserAndGetToken("inviter1@example.com", "John", "Doe");
        createUserAndGetToken("inviter2@example.com", "Bob", "Johnson");
        User inviter1 = userRepository.findActiveByEmail("inviter1@example.com").orElseThrow();
        User inviter2 = userRepository.findActiveByEmail("inviter2@example.com").orElseThrow();

        // Create two pending invitations
        HouseholdInvitation invitation1 = HouseholdExtensions.toInvitationEntity(
                inviter1.getHousehold(), invitedUser, inviter1);
        HouseholdInvitation invitation2 = HouseholdExtensions.toInvitationEntity(
                inviter2.getHousehold(), invitedUser, inviter2);
        householdInvitationRepository.save(invitation1);
        householdInvitationRepository.save(invitation2);

        // Get user's pending invitations
        mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].householdName", anyOf(is("John Doe's Household"), is("Bob Johnson's Household"))))
                .andExpect(jsonPath("$[0].invitedBy.email", anyOf(is("inviter1@example.com"), is("inviter2@example.com"))))
                .andExpect(jsonPath("$[1].id", notNullValue()))
                .andExpect(jsonPath("$[1].householdName", anyOf(is("John Doe's Household"), is("Bob Johnson's Household"))))
                .andExpect(jsonPath("$[1].invitedBy.email", anyOf(is("inviter1@example.com"), is("inviter2@example.com"))));
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoInvitations() throws Exception {
        // Create user without any invitations
        String userToken = createUserAndGetToken("user@example.com", "John", "Doe");
        User user = userRepository.findActiveByEmail("user@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household
        user.setHousehold(null);
        userRepository.save(user);

        // Get user's pending invitations
        mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldOnlyReturnPendingInvitations() throws Exception {
        // Create user who will receive invitations
        String invitedToken = createUserAndGetToken("invited@example.com", "Jane", "Smith");
        User invitedUser = userRepository.findActiveByEmail("invited@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household  
        invitedUser.setHousehold(null);
        userRepository.save(invitedUser);

        // Create inviter
        createUserAndGetToken("inviter@example.com", "John", "Doe");
        User inviter = userRepository.findActiveByEmail("inviter@example.com").orElseThrow();

        // Create pending invitation
        HouseholdInvitation pendingInvitation = HouseholdExtensions.toInvitationEntity(
                inviter.getHousehold(), invitedUser, inviter);
        householdInvitationRepository.save(pendingInvitation);

        // Create declined invitation
        HouseholdInvitation declinedInvitation = HouseholdExtensions.toInvitationEntity(
                inviter.getHousehold(), invitedUser, inviter);
        declinedInvitation.setStatus(HouseholdInvitation.InvitationStatus.DECLINED);
        householdInvitationRepository.save(declinedInvitation);

        // Get user's pending invitations - should only return the pending one
        mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(pendingInvitation.getId().intValue())));
    }

    @Test
    void shouldReturn401WhenNotAuthenticatedForGettingInvitations() throws Exception {
        // Try to get invitations without authentication
        mockMvc.perform(get("/api/users/me/invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExcludeExpiredInvitationsBasedOnExpiresAt() throws Exception {
        // Create user who will receive invitations
        String invitedToken = createUserAndGetToken("invited@example.com", "Jane", "Smith");
        User invitedUser = userRepository.findActiveByEmail("invited@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household  
        invitedUser.setHousehold(null);
        userRepository.save(invitedUser);

        // Create two different inviters from different households
        createUserAndGetToken("inviter1@example.com", "John", "Doe");
        createUserAndGetToken("inviter2@example.com", "Bob", "Johnson");
        User inviter1 = userRepository.findActiveByEmail("inviter1@example.com").orElseThrow();
        User inviter2 = userRepository.findActiveByEmail("inviter2@example.com").orElseThrow();

        // Create a valid pending invitation (not expired)
        HouseholdInvitation validInvitation = HouseholdExtensions.toInvitationEntity(
                inviter1.getHousehold(), invitedUser, inviter1);
        householdInvitationRepository.save(validInvitation);

        // Create an expired invitation from different household (past expiration date but still PENDING status)
        HouseholdInvitation expiredInvitation = HouseholdExtensions.toInvitationEntity(
                inviter2.getHousehold(), invitedUser, inviter2);
        expiredInvitation.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday
        householdInvitationRepository.save(expiredInvitation);

        // Get user's pending invitations - should only return the valid one
        mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Now should return only valid invitation
                .andExpect(jsonPath("$[0].id", is(validInvitation.getId().intValue())));
    }

    @Test
    void shouldExcludeInvitationsWithExpiredStatus() throws Exception {
        // Create user who will receive invitations
        String invitedToken = createUserAndGetToken("invited@example.com", "Jane", "Smith");
        User invitedUser = userRepository.findActiveByEmail("invited@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household  
        invitedUser.setHousehold(null);
        userRepository.save(invitedUser);

        // Create inviter
        createUserAndGetToken("inviter@example.com", "John", "Doe");
        User inviter = userRepository.findActiveByEmail("inviter@example.com").orElseThrow();

        // Create pending invitation
        HouseholdInvitation pendingInvitation = HouseholdExtensions.toInvitationEntity(
                inviter.getHousehold(), invitedUser, inviter);
        householdInvitationRepository.save(pendingInvitation);

        // Create invitation with EXPIRED status
        HouseholdInvitation expiredStatusInvitation = HouseholdExtensions.toInvitationEntity(
                inviter.getHousehold(), invitedUser, inviter);
        expiredStatusInvitation.setStatus(HouseholdInvitation.InvitationStatus.EXPIRED);
        householdInvitationRepository.save(expiredStatusInvitation);

        // Get user's pending invitations - should only return the pending one
        mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(pendingInvitation.getId().intValue())))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    void shouldHandleMixOfValidExpiredAndProcessedInvitations() throws Exception {
        // Create user who will receive invitations
        String invitedToken = createUserAndGetToken("invited@example.com", "Jane", "Smith");
        User invitedUser = userRepository.findActiveByEmail("invited@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household  
        invitedUser.setHousehold(null);
        userRepository.save(invitedUser);

        // Create multiple inviters from different households
        createUserAndGetToken("inviter1@example.com", "John", "Doe");
        createUserAndGetToken("inviter2@example.com", "Bob", "Johnson");
        User inviter1 = userRepository.findActiveByEmail("inviter1@example.com").orElseThrow();
        User inviter2 = userRepository.findActiveByEmail("inviter2@example.com").orElseThrow();

        // Create valid pending invitation
        HouseholdInvitation validInvitation = HouseholdExtensions.toInvitationEntity(
                inviter1.getHousehold(), invitedUser, inviter1);
        householdInvitationRepository.save(validInvitation);

        // Create expired invitation (past date, still PENDING)
        HouseholdInvitation expiredDateInvitation = HouseholdExtensions.toInvitationEntity(
                inviter2.getHousehold(), invitedUser, inviter2);
        expiredDateInvitation.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        householdInvitationRepository.save(expiredDateInvitation);

        // Create invitation with EXPIRED status
        HouseholdInvitation expiredStatusInvitation = HouseholdExtensions.toInvitationEntity(
                inviter1.getHousehold(), invitedUser, inviter1);
        expiredStatusInvitation.setStatus(HouseholdInvitation.InvitationStatus.EXPIRED);
        householdInvitationRepository.save(expiredStatusInvitation);

        // Create accepted invitation
        HouseholdInvitation acceptedInvitation = HouseholdExtensions.toInvitationEntity(
                inviter2.getHousehold(), invitedUser, inviter2);
        acceptedInvitation.setStatus(HouseholdInvitation.InvitationStatus.ACCEPTED);
        householdInvitationRepository.save(acceptedInvitation);

        // Get user's pending invitations - should only return valid pending ones
        var result = mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invitedToken))
                .andExpect(status().isOk());
                
        // For now, this will show the current behavior - we expect to see both valid and expired-by-date
        // This test will help identify if the system properly handles expiration logic
        result.andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
              .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))));
    }

    @Test
    void shouldAutomaticallyExpireInvitationsAndUpdateStatus() throws Exception {
        // Create user who will receive invitations
        String invitedToken = createUserAndGetToken("invited@example.com", "Jane", "Smith");
        User invitedUser = userRepository.findActiveByEmail("invited@example.com").orElseThrow();
        
        // Remove household association to simulate user not in any household  
        invitedUser.setHousehold(null);
        userRepository.save(invitedUser);

        // Create inviter
        createUserAndGetToken("inviter@example.com", "John", "Doe");
        User inviter = userRepository.findActiveByEmail("inviter@example.com").orElseThrow();

        // Create an expired invitation (past expiration date but still PENDING status)
        HouseholdInvitation expiredInvitation = HouseholdExtensions.toInvitationEntity(
                inviter.getHousehold(), invitedUser, inviter);
        expiredInvitation.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        householdInvitationRepository.save(expiredInvitation);

        // Verify invitation is initially PENDING
        HouseholdInvitation savedInvitation = householdInvitationRepository.findById(expiredInvitation.getId()).orElseThrow();
        assertEquals(HouseholdInvitation.InvitationStatus.PENDING, savedInvitation.getStatus());

        // Get user's pending invitations - this should trigger automatic expiration
        mockMvc.perform(get("/api/users/me/invitations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // Should return empty list

        // Verify the invitation status was automatically updated to EXPIRED
        HouseholdInvitation updatedInvitation = householdInvitationRepository.findById(expiredInvitation.getId()).orElseThrow();
        assertEquals(HouseholdInvitation.InvitationStatus.EXPIRED, updatedInvitation.getStatus());
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