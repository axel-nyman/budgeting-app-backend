package org.example.axelnyman.main.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
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

import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.extensions.HouseholdExtensions;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;

import java.time.LocalDateTime;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class HouseholdIntegrationTest {

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
        void shouldReturnHouseholdInformationWithActiveMembers() throws Exception {
                // Create household with multiple users
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User firstUser = userRepository.findAll().get(0);
                Household household = firstUser.getHousehold();

                // Add second active user to same household
                User secondUser = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "password123",
                                household);
                userRepository.save(secondUser);

                // Add third active user to same household
                User thirdUser = new User(
                                "Bob",
                                "Johnson",
                                "bob.johnson@example.com",
                                "password123",
                                household);
                userRepository.save(thirdUser);

                // Test GET /api/households
                mockMvc.perform(get("/api/households")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(household.getId().intValue())))
                                .andExpect(jsonPath("$.name", is(household.getName())))
                                .andExpect(jsonPath("$.createdAt", notNullValue()))
                                .andExpect(jsonPath("$.memberCount", is(3)))
                                .andExpect(jsonPath("$.members", hasSize(3)))
                                .andExpect(jsonPath("$.members[0].id", notNullValue()))
                                .andExpect(jsonPath("$.members[0].firstName", anyOf(is("John"), is("Jane"), is("Bob"))))
                                .andExpect(jsonPath("$.members[0].lastName",
                                                anyOf(is("Doe"), is("Smith"), is("Johnson"))))
                                .andExpect(jsonPath("$.members[0].email",
                                                anyOf(is("john.doe@example.com"), is("jane.smith@example.com"),
                                                                is("bob.johnson@example.com"))))
                                .andExpect(jsonPath("$.members[0].joinedAt", notNullValue()));
        }

        @Test
        void shouldExcludeSoftDeletedUsersFromMemberList() throws Exception {
                // Create household with multiple users
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User firstUser = userRepository.findAll().get(0);
                Household household = firstUser.getHousehold();

                // Add second active user
                User secondUser = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "password123",
                                household);
                userRepository.save(secondUser);

                // Add third user and soft delete them
                User thirdUser = new User(
                                "Bob",
                                "Johnson",
                                "bob.johnson@example.com",
                                "password123",
                                household);
                thirdUser.setDeletedAt(LocalDateTime.now());
                userRepository.save(thirdUser);

                // Test that soft-deleted user is excluded
                mockMvc.perform(get("/api/households")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.memberCount", is(2)))
                                .andExpect(jsonPath("$.members", hasSize(2)))
                                .andExpect(jsonPath("$.members[*].email", not(hasItem("bob.johnson@example.com"))));
        }

        @Test
        void shouldReturn401WhenNotAuthenticated() throws Exception {
                // Test GET /api/households without token
                mockMvc.perform(get("/api/households")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn404WhenHouseholdNotFound() throws Exception {
                // This would be a rare edge case where user exists but household is deleted
                // Creating a minimal test scenario
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User user = userRepository.findAll().get(0);

                // Delete the household (this would be an edge case in real application)
                householdRepository.delete(user.getHousehold());

                mockMvc.perform(get("/api/households")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnAccurateMemberCount() throws Exception {
                // Create household with exactly 5 users
                String token = createUserAndGetToken("user1@example.com", "User", "One");
                User firstUser = userRepository.findAll().get(0);
                Household household = firstUser.getHousehold();

                // Add 4 more users
                for (int i = 2; i <= 5; i++) {
                        User user = new User(
                                        "User",
                                        "Number" + i,
                                        "user" + i + "@example.com",
                                        "password123",
                                        household);
                        userRepository.save(user);
                }

                mockMvc.perform(get("/api/households")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.memberCount", is(5)))
                                .andExpect(jsonPath("$.members", hasSize(5)));
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
}