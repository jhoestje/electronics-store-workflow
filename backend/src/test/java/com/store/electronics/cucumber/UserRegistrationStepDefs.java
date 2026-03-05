package com.store.electronics.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.electronics.dto.RegisterRequest;
import com.store.electronics.repository.UserRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class UserRegistrationStepDefs {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private MvcResult lastResult;

    @Before
    public void resetDatabase() {
        userRepository.deleteAll();
    }

    @Given("the user database is empty")
    public void theDatabaseIsEmpty() {
        userRepository.deleteAll();
    }

    @Given("a user is already registered with username {string}, email {string}, and password {string}")
    public void aUserIsAlreadyRegistered(String username, String email, String password) throws Exception {
        RegisterRequest request = buildRequest(username, email, password);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @When("I submit a registration with username {string}, email {string}, and password {string}")
    public void iSubmitRegistration(String username, String email, String password) throws Exception {
        RegisterRequest request = buildRequest(username, email, password);
        lastResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    @Then("the registration should succeed with status {int}")
    public void theRegistrationShouldSucceed(int status) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the registration should fail with status {int}")
    public void theRegistrationShouldFail(int status) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(status);
    }

    @And("the response should contain a JWT token")
    public void theResponseShouldContainJwt() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains("token");
        String token = objectMapper.readTree(body).get("token").asText();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @And("the response should contain user with username {string}")
    public void theResponseShouldContainUser(String username) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        String actualUsername = objectMapper.readTree(body).path("user").path("username").asText();
        assertThat(actualUsername).isEqualTo(username);
    }

    @And("the response should not expose the password")
    public void theResponseShouldNotExposePassword() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).path("user").path("password").isMissingNode()).isTrue();
    }

    @And("the response user should have role {string}")
    public void theResponseUserShouldHaveRole(String role) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        String rolesJson = objectMapper.readTree(body).path("user").path("roles").toString();
        assertThat(rolesJson).contains(role);
    }

    @And("the response should contain error {string}")
    public void theResponseShouldContainError(String errorMessage) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        String error = objectMapper.readTree(body).get("error").asText();
        assertThat(error).isEqualTo(errorMessage);
    }

    @And("the response should contain a password validation error")
    public void theResponseShouldContainPasswordValidationError() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains("password");
    }

    private RegisterRequest buildRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
