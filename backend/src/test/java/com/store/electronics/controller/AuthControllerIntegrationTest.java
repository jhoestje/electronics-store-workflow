package com.store.electronics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.electronics.dto.AuthRequest;
import com.store.electronics.dto.RegisterRequest;
import com.store.electronics.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=c2VjcmV0S2V5Rm9yRWxlY3Ryb25pY3NTdG9yZUFwcGxpY2F0aW9u",
        "jwt.expiration=86400000"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void register_givenValidRequest_shouldReturn200WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("johndoe"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void register_givenFirstUser_shouldReturn200WithAdminRole() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("firstadmin");
        request.setEmail("admin@example.com");
        request.setPassword("Admin1@secure");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void register_givenDuplicateUsername_shouldReturn400WithErrorMessage() throws Exception {
        RegisterRequest first = new RegisterRequest();
        first.setUsername("johndoe");
        first.setEmail("john1@example.com");
        first.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)));

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("johndoe");
        duplicate.setEmail("john2@example.com");
        duplicate.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_givenDuplicateEmail_shouldReturn400WithErrorMessage() throws Exception {
        RegisterRequest first = new RegisterRequest();
        first.setUsername("johndoe");
        first.setEmail("shared@example.com");
        first.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)));

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("janedoe");
        duplicate.setEmail("shared@example.com");
        duplicate.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void register_givenWeakPassword_shouldReturn400WithValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("weakpassword");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void register_givenBlankUsername_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("john@example.com");
        request.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    void register_givenInvalidEmail_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("not-an-email");
        request.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void login_givenValidCredentials_shouldReturn200WithToken() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("loginuser");
        reg.setEmail("login@example.com");
        reg.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("loginuser"));
    }

    @Test
    void login_givenInvalidPassword_shouldReturn401() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("loginuser2");
        reg.setEmail("login2@example.com");
        reg.setPassword("Secure1@pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("loginuser2");
        loginRequest.setPassword("WrongPass1@");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError());
    }
}
