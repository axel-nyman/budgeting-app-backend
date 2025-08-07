package org.example.axelnyman.main.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.example.axelnyman.main.infrastructure.data.context.HouseholdRepository;

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
        void shouldGetUserById() throws Exception {
                Household household = new Household("Test Household");
                Household savedHousehold = householdRepository.save(household);
                
                User user = new User(
                                "John",
                                "Doe",
                                "john.doe@example.com",
                                "hashedPassword123",
                                savedHousehold);
                User savedUser = userRepository.save(user);

                mockMvc.perform(get("/api/users/" + savedUser.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(savedUser.getId().intValue())))
                                .andExpect(jsonPath("$.email", is("john.doe@example.com")));
        }

        @Test
        void shouldGetAllUsers() throws Exception {
                Household household1 = new Household("Test Household 1");
                Household savedHousehold1 = householdRepository.save(household1);
                
                Household household2 = new Household("Test Household 2");
                Household savedHousehold2 = householdRepository.save(household2);
                
                User user1 = new User(
                                "John",
                                "Doe",
                                "john.doe@example.com",
                                "hashedPassword123",
                                savedHousehold1);

                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com",
                                "hashedPassword456",
                                savedHousehold2);

                userRepository.save(user1);
                userRepository.save(user2);

                mockMvc.perform(get("/api/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void shouldDeleteUser() throws Exception {
                Household household = new Household("Test Household");
                Household savedHousehold = householdRepository.save(household);
                
                User user = new User(
                                "John",
                                "Doe",
                                "john.doe@example.com",
                                "hashedPassword123",
                                savedHousehold);
                User savedUser = userRepository.save(user);

                mockMvc.perform(delete("/api/users/" + savedUser.getId()))
                                .andExpect(status().isNoContent());

        }
}
