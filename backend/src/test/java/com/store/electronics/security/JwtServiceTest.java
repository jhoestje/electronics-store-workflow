package com.store.electronics.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "c2VjcmV0S2V5Rm9yRWxlY3Ryb25pY3NTdG9yZUFwcGxpY2F0aW9u";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    private UserDetails buildUserDetails(String username) {
        return User.withUsername(username)
                .password("hashed")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void generateToken_givenValidUserDetails_shouldReturnNonBlankToken() {
        UserDetails userDetails = buildUserDetails("johndoe");

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_givenValidToken_shouldReturnCorrectUsername() {
        UserDetails userDetails = buildUserDetails("johndoe");
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("johndoe");
    }

    @Test
    void isTokenValid_givenValidTokenAndMatchingUser_shouldReturnTrue() {
        UserDetails userDetails = buildUserDetails("johndoe");
        String token = jwtService.generateToken(userDetails);

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_givenTokenWithDifferentUser_shouldReturnFalse() {
        UserDetails tokenOwner = buildUserDetails("johndoe");
        UserDetails otherUser = buildUserDetails("janedoe");
        String token = jwtService.generateToken(tokenOwner);

        boolean valid = jwtService.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_givenExpiredToken_shouldReturnFalse() {
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secretKey", SECRET);
        ReflectionTestUtils.setField(shortLivedService, "jwtExpiration", -1000L);

        UserDetails userDetails = buildUserDetails("johndoe");
        String expiredToken = shortLivedService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void generateToken_givenTwoDifferentUsers_shouldReturnDifferentTokens() {
        UserDetails user1 = buildUserDetails("alice");
        UserDetails user2 = buildUserDetails("bob");

        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        assertThat(token1).isNotEqualTo(token2);
    }
}
