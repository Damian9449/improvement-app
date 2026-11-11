package com.improvement_app.common.ratelimit;

import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.security.request.LoginRequest;
import com.improvement_app.security.services.EmailService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for {@link RateLimitFilter} — sprawdzają, że filtr jest faktycznie podpięty
 * w łańcuchu Spring Security (WebSecurityConfig) i działa na prawdziwym serwerze.
 * Logika limitów jest pokryta w {@link RateLimitFilterTest}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.security.rate-limit.http.auth-per-minute=2",
                "app.security.rate-limit.http.default-per-minute=5"
        })
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("RateLimitFilter Integration")
class RateLimitFilterIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean private EmailService emailService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("POST /signin ponad limit → 429 z JSON-em i Retry-After")
    void signinShouldBeRateLimited() {
        for (int i = 0; i < 2; i++) {
            signin().then().statusCode(not(equalTo(429)));
        }

        signin()
                .then()
                .statusCode(429)
                .contentType(ContentType.JSON)
                .header("Retry-After", notNullValue())
                .body("code", equalTo("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Filtr działa przed autoryzacją — anonimowe żądania do chronionego API też są limitowane")
    void anonymousRequestsToProtectedApiShouldBeRateLimited() {
        RestAssured.given().get("/api/v1/trainings")
                .then()
                .statusCode(401)
                .header(RateLimitFilter.REMAINING_HEADER, equalTo("4"));

        for (int i = 0; i < 4; i++) {
            RestAssured.given().get("/api/v1/trainings");
        }

        RestAssured.given().get("/api/v1/trainings").then().statusCode(429);
    }

    private io.restassured.response.Response signin() {
        return RestAssured
                .given()
                    .contentType(ContentType.JSON)
                    .body(new LoginRequest("nobody", "wrong-password"))
                .when()
                    .post("/api/v1/auth/signin");
    }
}
