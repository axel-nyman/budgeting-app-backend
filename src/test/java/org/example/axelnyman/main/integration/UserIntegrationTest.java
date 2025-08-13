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

import org.example.axelnyman.main.domain.dtos.UserDtos.*;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class UserIntegrationTest {

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
        void shouldGetUserByIdInSameHousehold() throws Exception {
                // Create first user and get token
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User firstUser = userRepository.findAll().get(0);
                Household firstHousehold = firstUser.getHousehold();

                // Create second user in same household
                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "hashedPassword456",
                                firstHousehold);
                User savedUser2 = userRepository.save(user2);

                mockMvc.perform(get("/api/users/" + savedUser2.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(savedUser2.getId().intValue())))
                                .andExpect(jsonPath("$.firstName", is("Jane")))
                                .andExpect(jsonPath("$.lastName", is("Smith")))
                                .andExpect(jsonPath("$.email", is("jane.smith@example.com")))
                                // Ensure password fields are never included
                                .andExpect(jsonPath("$.password").doesNotExist())
                                .andExpect(jsonPath("$.hashedPassword").doesNotExist());
        }

        @Test
        void shouldReturn404ForUserInDifferentHousehold() throws Exception {
                // Create first user and get token
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");

                // Create second user in different household
                Household household2 = new Household("Different Household");
                Household savedHousehold2 = householdRepository.save(household2);
                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "hashedPassword456",
                                savedHousehold2);
                User savedUser2 = userRepository.save(user2);

                mockMvc.perform(get("/api/users/" + savedUser2.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404ForSoftDeletedUser() throws Exception {
                // Create first user and get token
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User firstUser = userRepository.findAll().get(0);
                Household firstHousehold = firstUser.getHousehold();

                // Create second user in same household
                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "hashedPassword456",
                                firstHousehold);
                User savedUser2 = userRepository.save(user2);

                // Soft delete the second user
                savedUser2.setDeletedAt(java.time.LocalDateTime.now());
                userRepository.save(savedUser2);

                mockMvc.perform(get("/api/users/" + savedUser2.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404ForNonExistentUser() throws Exception {
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                Long nonExistentId = 99999L;

                mockMvc.perform(get("/api/users/" + nonExistentId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldGetHouseholdUsers() throws Exception {
                // Create first user and get token
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User firstUser = userRepository.findAll().get(0);
                Household firstHousehold = firstUser.getHousehold();

                // Create second user in same household
                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "hashedPassword456",
                                firstHousehold);
                userRepository.save(user2);

                // Create third user in different household
                Household household2 = new Household("Different Household");
                Household savedHousehold2 = householdRepository.save(household2);
                User user3 = new User(
                                "Bob",
                                "Wilson",
                                "bob.wilson@example.com",
                                "hashedPassword789",
                                savedHousehold2);
                userRepository.save(user3);

                mockMvc.perform(get("/api/users")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].firstName", anyOf(is("John"), is("Jane"))))
                                .andExpect(jsonPath("$[1].firstName", anyOf(is("John"), is("Jane"))))
                                // Ensure password fields are never included
                                .andExpect(jsonPath("$[0].password").doesNotExist())
                                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist())
                                .andExpect(jsonPath("$[1].password").doesNotExist())
                                .andExpect(jsonPath("$[1].hashedPassword").doesNotExist());
        }

        @Test
        void shouldExcludeSoftDeletedUsersFromHouseholdUsers() throws Exception {
                // Create first user and get token
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User firstUser = userRepository.findAll().get(0);
                Household firstHousehold = firstUser.getHousehold();

                // Create second user in same household
                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "hashedPassword456",
                                firstHousehold);
                userRepository.save(user2);

                // Soft delete the second user
                user2.setDeletedAt(java.time.LocalDateTime.now());
                userRepository.save(user2);

                mockMvc.perform(get("/api/users")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].firstName", is("John")));
        }

        @Test
        void shouldOnlyReturnUsersFromAuthenticatedUserHousehold() throws Exception {
                // Create two separate households with users
                String token1 = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                String token2 = createUserAndGetToken("jane.smith@example.com", "Jane", "Smith");

                // Each user should only see their own household users
                mockMvc.perform(get("/api/users")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].firstName", is("John")));

                mockMvc.perform(get("/api/users")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].firstName", is("Jane")));
        }

        @Test
        void shouldGetCurrentUserProfile() throws Exception {
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User savedUser = userRepository.findAll().get(0);

                mockMvc.perform(get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(savedUser.getId().intValue())))
                                .andExpect(jsonPath("$.firstName", is("John")))
                                .andExpect(jsonPath("$.lastName", is("Doe")))
                                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                                .andExpect(jsonPath("$.household").exists())
                                .andExpect(jsonPath("$.household.id", is(savedUser.getHousehold().getId().intValue())))
                                .andExpect(jsonPath("$.household.name").exists())
                                .andExpect(jsonPath("$.createdAt").exists())
                                // Ensure password is never included in response
                                .andExpect(jsonPath("$.password").doesNotExist())
                                .andExpect(jsonPath("$.hashedPassword").doesNotExist());
        }

        @Test
        void shouldReturn401WhenNotAuthenticated() throws Exception {
                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldDeleteUser() throws Exception {
                String token = createUserAndGetToken("john.doe@example.com", "John", "Doe");
                User savedUser = userRepository.findAll().get(0);

                mockMvc.perform(delete("/api/users/" + savedUser.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                                .andExpect(status().isNoContent());
        }

        private String createUserAndGetToken(String email, String firstName, String lastName) throws Exception {
                RegisterRequest request = new RegisterRequest(firstName, lastName, email, "password123");

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
