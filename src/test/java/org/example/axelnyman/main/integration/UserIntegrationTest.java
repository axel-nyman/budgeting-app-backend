package org.example.axelnyman.main.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import org.example.axelnyman.main.domain.dtos.UserDtos.CreateUserRequest;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.infrastructure.data.context.UserRepository;
import org.springframework.test.web.servlet.MvcResult;

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
        private ObjectMapper objectMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                userRepository.deleteAll();
        }

        @AfterAll
        static void cleanup() {
                if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
                        postgreSQLContainer.stop();
                }
        }

        @Test
        void shouldRegisterNewUser() throws Exception {
                CreateUserRequest request = new CreateUserRequest(
                                "John", "Doe", "john.doe@example.com");

                MvcResult result = mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(request().asyncStarted())
                                .andExpect(request().asyncResult(notNullValue()))
                                .andReturn();

                mockMvc.perform(asyncDispatch(result))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.firstName", is("John")))
                                .andExpect(jsonPath("$.lastName", is("Doe")))
                                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                                .andExpect(jsonPath("$.id", notNullValue()));
        }

        @Test
        void shouldNotRegisterUserWithDuplicateEmail() throws Exception {
                User existingUser = new User(
                                "Jane",
                                "Doe",
                                "jane.doe@example.com");

                userRepository.save(existingUser);

                CreateUserRequest request = new CreateUserRequest(
                                "John",
                                "Doe",
                                "jane.doe@example.com");

                MvcResult result = mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(request().asyncStarted())
                                .andExpect(request().asyncResult(notNullValue()))
                                .andReturn();

                mockMvc.perform(asyncDispatch(result))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldGetUserById() throws Exception {
                User user = new User(
                                "John",
                                "Doe",
                                "john.doe@example.com");
                User savedUser = userRepository.save(user);

                MvcResult result = mockMvc.perform(get("/api/users/" + savedUser.getId()))
                                .andExpect(request().asyncStarted())
                                .andExpect(request().asyncResult(notNullValue()))
                                .andReturn();

                mockMvc.perform(asyncDispatch(result))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(savedUser.getId().intValue())))
                                .andExpect(jsonPath("$.email", is("john.doe@example.com")));
        }

        @Test
        void shouldGetAllUsers() throws Exception {
                User user1 = new User(
                                "John",
                                "Doe",
                                "john.doe@example.com");

                User user2 = new User(
                                "Jane",
                                "Smith",
                                "jane.smith@example.com");

                userRepository.save(user1);
                userRepository.save(user2);

                MvcResult result = mockMvc.perform(get("/api/users"))
                                .andExpect(request().asyncStarted())
                                .andExpect(request().asyncResult(notNullValue()))
                                .andReturn();

                mockMvc.perform(asyncDispatch(result))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void shouldDeleteUser() throws Exception {
                User user = new User(
                                "John",
                                "Doe",
                                "john.doe@example.com");
                User savedUser = userRepository.save(user);

                MvcResult result = mockMvc.perform(delete("/api/users/" + savedUser.getId()))
                                .andExpect(request().asyncStarted())
                                .andExpect(request().asyncResult(notNullValue()))
                                .andReturn();

                mockMvc.perform(asyncDispatch(result))
                                .andExpect(status().isNoContent());

        }
}
