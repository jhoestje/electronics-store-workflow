package com.store.electronics.service;

import com.store.electronics.dto.AuthRequest;
import com.store.electronics.dto.AuthResponse;
import com.store.electronics.dto.RegisterRequest;
import com.store.electronics.exception.EmailAlreadyExistsException;
import com.store.electronics.exception.UsernameAlreadyExistsException;
import com.store.electronics.model.User;
import com.store.electronics.repository.UserRepository;
import com.store.electronics.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("johndoe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("Secure1@pass");
    }

    @Test
    void register_givenValidRequest_shouldReturnAuthResponseWithToken() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("johndoe");
        savedUser.setPassword("hashed");
        savedUser.setEmail("john@example.com");
        savedUser.setRoles(Set.of("ROLE_CUSTOMER"));

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secure1@pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.count()).thenReturn(2L);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo("johndoe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_givenFirstUser_shouldPromoteToAdminRole() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("johndoe");
        savedUser.setPassword("hashed");
        savedUser.setEmail("john@example.com");
        savedUser.setRoles(Set.of("ROLE_CUSTOMER"));

        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("johndoe");
        adminUser.setPassword("hashed");
        adminUser.setEmail("john@example.com");
        adminUser.setRoles(Set.of("ROLE_ADMIN"));

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser).thenReturn(adminUser);
        when(userRepository.count()).thenReturn(1L);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        verify(userRepository, times(2)).save(any(User.class));
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void register_givenExistingUsername_shouldThrowUsernameAlreadyExistsException() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_givenExistingEmail_shouldThrowEmailAlreadyExistsException() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_givenValidCredentials_shouldReturnAuthResponseWithToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("johndoe");
        user.setPassword("hashed");
        user.setRoles(Set.of("ROLE_CUSTOMER"));

        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("johndoe");
        authRequest.setPassword("Secure1@pass");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(authRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo("johndoe");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_givenInvalidCredentials_shouldThrowException() {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("johndoe");
        authRequest.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(authRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
