package com.improvement_app.security.config;

import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.security.services.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprawdza, że handshake SockJS (/ws/**) przechodzi przez prawdziwy łańcuch Spring Security
 * (bez PermitAllSecurityConfig) i nie jest już publiczny.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("WebSocket security")
class WebSocketSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmailService emailService;

    @Test
    @DisplayName("GET /ws/info bez logowania → 401")
    void anonymousHandshakeShouldBeRejected() throws Exception {
        mockMvc.perform(get("/ws/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /ws/info zalogowanego użytkownika → 200")
    void authenticatedHandshakeShouldBeAllowed() throws Exception {
        mockMvc.perform(get("/ws/info"))
                .andExpect(status().isOk());
    }

}
