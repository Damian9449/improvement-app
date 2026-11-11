package com.improvement_app.security.controllers;

import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.security.entity.ERole;
import com.improvement_app.security.entity.Role;
import com.improvement_app.security.entity.TokenTypeEnum;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.entity.UserTokenEntity;
import com.improvement_app.security.repository.RoleRepository;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.security.repository.UserTokenRepository;
import com.improvement_app.security.request.LoginRequest;
import com.improvement_app.security.request.ResendVerificationEmailRequest;
import com.improvement_app.security.request.SignupRequest;
import com.improvement_app.security.services.EmailService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Integration tests for {@link AuthController} — pełen kontekst Springa, RestAssured,
 * realna baza w testcontainerze. EmailService jest mockowany aby nie wysyłać maili.
 *
 * Sprawdzamy ścieżkę REST end-to-end:
 *  • /signup       — rejestracja + wysyłka emaila weryfikacyjnego
 *  • /signin       — logowanie i ustawianie cookies
 *  • /verify-email — aktywacja konta tokenem
 *  • /logout       — czyszczenie cookies
 *  • /refresh-token, /resend-verification — przypadki błędne
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("AuthController Integration")
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserTokenRepository userTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private EmailService emailService; // nie wysyłamy realnych maili

    private Role userRole;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/auth";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Rola ROLE_USER musi istnieć (signup szuka jej przez RoleRepository)
        userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_USER)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserEntity persistVerifiedUser(String username, String email, String rawPassword) {
        UserEntity u = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name("Jan")
                .surname("Kowalski")
                .emailVerified(true)
                .isActive(true)
                .failedLoginAttempts(0)
                .roles(Set.of(userRole))
                .build();
        return userRepository.save(u);
    }

    private UserEntity persistUnverifiedUser(String username, String email, String rawPassword) {
        UserEntity u = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name("Jan")
                .surname("Kowalski")
                .emailVerified(false)
                .isActive(false)
                .failedLoginAttempts(0)
                .roles(Set.of(userRole))
                .build();
        return userRepository.save(u);
    }

    private SignupRequest signupRequest(String username, String email) {
        SignupRequest r = new SignupRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword("Password1!");
        r.setName("Jan");
        r.setSurname("Kowalski");
        return r;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /signup
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /signup → 200, zapisuje NIEAKTYWNEGO usera i wysyła email weryfikacyjny")
    void signupShouldCreateInactiveUserAndSendVerificationEmail() {
        // when / then
        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(signupRequest("newuser", "newuser@example.com"))
                .when()
                    .post("/signup")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("message", containsString("registered successfully"));

        // then – stan bazy (kluczowe asercje bezpieczeństwa)
        UserEntity saved = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(saved.getEmailVerified()).isFalse();        // niezweryfikowany do potwierdzenia maila
        assertThat(saved.getIsActive()).isFalse();             // konto nieaktywne
        assertThat(saved.getPassword()).isNotEqualTo("Password1!"); // hasło zahashowane
        assertThat(passwordEncoder.matches("Password1!", saved.getPassword())).isTrue();

        // then – wysłany email
        verify(emailService).sendVerificationEmail(eq("newuser@example.com"), any(UserTokenEntity.class));
    }

    @Test
    @DisplayName("POST /signup z zajętym username → 400 USER_ALREADY_EXISTS, brak wysyłki maila")
    void signupShouldFailWhenUsernameTaken() {
        persistVerifiedUser("dup", "dup@example.com", "Password1!");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(signupRequest("dup", "other@example.com"))
                .when()
                    .post("/signup")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("USER_ALREADY_EXISTS"));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("POST /signup z zajętym email → 400 USER_ALREADY_EXISTS")
    void signupShouldFailWhenEmailTaken() {
        persistVerifiedUser("user1", "taken@example.com", "Password1!");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(signupRequest("user2", "taken@example.com"))
                .when()
                    .post("/signup")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("USER_ALREADY_EXISTS"));

        verifyNoInteractions(emailService);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /signin
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /signin z poprawnymi danymi → 200 + access/refresh w cookies + dane usera")
    void signinShouldReturnTokensInCookiesForValidUser() {
        persistVerifiedUser("alice", "alice@example.com", "Password1!");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(new LoginRequest("alice", "Password1!"))
                .when()
                    .post("/signin")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .cookie("access_token", not(emptyOrNullString()))
                    .cookie("refresh_token", not(emptyOrNullString()))
                    .body("username", equalTo("alice"))
                    .body("email", equalTo("alice@example.com"))
                    .body("roles", hasItem("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /signin z błędnym hasłem → 401 INVALID_CREDENTIALS")
    void signinShouldReturn401ForBadPassword() {
        persistVerifiedUser("alice", "alice@example.com", "Password1!");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(new LoginRequest("alice", "wrong-password"))
                .when()
                    .post("/signin")
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .body("code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /signin dla nieistniejącego usera → 401 INVALID_CREDENTIALS (nie ujawnia czy user istnieje)")
    void signinShouldReturn401ForUnknownUser() {
        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(new LoginRequest("ghost", "Password1!"))
                .when()
                    .post("/signin")
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .body("code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /signin z niezweryfikowanym mailem → 403 EMAIL_NOT_VERIFIED, brak tokenów")
    void signinShouldReturn403WhenEmailNotVerified() {
        persistUnverifiedUser("bob", "bob@example.com", "Password1!");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(new LoginRequest("bob", "Password1!"))
                .when()
                    .post("/signin")
                .then()
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .body("code", equalTo("EMAIL_NOT_VERIFIED"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /verify-email
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /verify-email z poprawnym tokenem → 200, user aktywowany w bazie")
    void verifyEmailShouldActivateUser() {
        UserEntity user = persistUnverifiedUser("charlie", "charlie@example.com", "Password1!");

        UserTokenEntity token = UserTokenEntity.builder()
                .user(user)
                .token("verify-token-123")
                .type(TokenTypeEnum.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        userTokenRepository.save(token);

        RestAssured
                .given()
                .when()
                    .get("/verify-email?token=verify-token-123")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("message", containsString("Email verified"));

        UserEntity refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.getEmailVerified()).isTrue();
        assertThat(refreshed.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("GET /verify-email z nieistniejącym tokenem → 400 INVALID_TOKEN")
    void verifyEmailShouldReturn400ForInvalidToken() {
        RestAssured
                .given()
                .when()
                    .get("/verify-email?token=does-not-exist")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("GET /verify-email z przeterminowanym tokenem → 4xx, user nadal nieaktywny")
    void verifyEmailShouldRejectExpiredToken() {
        UserEntity user = persistUnverifiedUser("dave", "dave@example.com", "Password1!");

        UserTokenEntity expired = UserTokenEntity.builder()
                .user(user)
                .token("expired-token")
                .type(TokenTypeEnum.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        userTokenRepository.save(expired);

        RestAssured
                .given()
                .when()
                    .get("/verify-email?token=expired-token")
                .then()
                    .statusCode(org.hamcrest.Matchers.greaterThanOrEqualTo(400));

        UserEntity refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.getEmailVerified()).isFalse();
        assertThat(refreshed.getIsActive()).isFalse();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /resend-verification
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /resend-verification dla istniejącego usera → 200 + ponowna wysyłka maila")
    void resendVerificationShouldSendEmailForExistingUser() {
        persistUnverifiedUser("eve", "eve@example.com", "Password1!");

        ResendVerificationEmailRequest req = new ResendVerificationEmailRequest();
        req.setUsername("eve");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(req)
                .when()
                    .post("/resend-verification")
                .then()
                    .statusCode(HttpStatus.OK.value());

        verify(emailService).sendVerificationEmail(eq("eve@example.com"), any(UserTokenEntity.class));
    }

    @Test
    @DisplayName("POST /resend-verification dla nieznanego usera → 400, brak wysyłki maila")
    void resendVerificationShouldFailForUnknownUser() {
        ResendVerificationEmailRequest req = new ResendVerificationEmailRequest();
        req.setUsername("nobody");

        RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(req)
                .when()
                    .post("/resend-verification")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());

        verifyNoInteractions(emailService);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /logout
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /logout → 204 + Set-Cookie czyszczące access/refresh")
    void logoutShouldClearAuthCookies() {
        RestAssured
                .given()
                .when()
                    .post("/logout")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value())
                    // oba cookies wyzerowane: wartość pusta + Max-Age=0 (każdy w osobnym Set-Cookie)
                    .cookie("access_token", equalTo(""))
                    .cookie("refresh_token", equalTo(""));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /refresh-token
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /refresh-token bez cookie → 401 UNAUTHORIZED")
    void refreshTokenShouldReturn401WithoutCookie() {
        RestAssured
                .given()
                .when()
                    .post("/refresh-token")
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /refresh-token z nieprawidłowym refresh tokenem → 401 UNAUTHORIZED")
    void refreshTokenShouldReturn401ForInvalidToken() {
        RestAssured
                .given()
                    .cookie("refresh_token", "not-a-valid-jwt")
                .when()
                    .post("/refresh-token")
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .body("code", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /refresh-token po /signin → 200 + nowy access token w cookie")
    void refreshTokenShouldReturnNewAccessTokenForValidRefreshCookie() {
        persistVerifiedUser("frank", "frank@example.com", "Password1!");

        // 1) zaloguj się — pobierz refresh_token z cookies
        String refreshCookie = RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(new LoginRequest("frank", "Password1!"))
                .when()
                    .post("/signin")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().cookie("refresh_token");

        assertThat(refreshCookie).isNotBlank();

        // 2) wyślij refresh-token z tym cookie
        RestAssured
                .given()
                    .cookie("refresh_token", refreshCookie)
                .when()
                    .post("/refresh-token")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .cookie("access_token", not(emptyOrNullString()))
                    .body("accessTokenExpiresAt", org.hamcrest.Matchers.greaterThan(System.currentTimeMillis()));
    }
}
