package com.improvement_app.other.daily;

import com.improvement_app.food.config.PermitAllSecurityConfig;
import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.other.daily.entity.DailyRecord;
import com.improvement_app.other.daily.repository.DailyRepository;
import com.improvement_app.other.daily.request.DailyRecordRequest;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.improvement_app.workouts.data.WorkoutTestDataFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E DailyController — zapis do Postgresa i izolacja danych między użytkownikami.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, PermitAllSecurityConfig.class})
@ActiveProfiles("test")
class DailyControllerE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DailyRepository dailyRepository;
    @Autowired private UserRepository userRepository;

    // Sprzątamy też po teście — inne klasy E2E robią userRepository.deleteAll(),
    // a wpisy daily trzymają FK do users.
    @BeforeEach
    @AfterEach
    void cleanDb() {
        dailyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /daily/ zapisuje wpis przypisany do zalogowanego użytkownika")
    void shouldSaveDailyForCurrentUser() throws Exception {
        UserEntity u = userRepository.save(user("daily1"));

        mockMvc.perform(post("/api/v1/daily/")
                        .with(authentication(authOf(u)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"health": true, "exercise": false, "book": true, "software": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value(true))
                .andExpect(jsonPath("$.exercise").value(false))
                .andExpect(jsonPath("$.book").value(true))
                .andExpect(jsonPath("$.software").value(true))
                .andExpect(jsonPath("$.user").doesNotExist());

        assertThat(dailyRepository.findAll())
                .singleElement()
                .satisfies(d -> assertThat(d.getUser().getId()).isEqualTo(u.getId()));
    }

    @Test
    @DisplayName("GET /daily zwraca tylko wpisy zalogowanego użytkownika")
    void shouldReturnOnlyOwnDailies() throws Exception {
        UserEntity owner = userRepository.save(user("daily-owner"));
        UserEntity other = userRepository.save(user("daily-other"));

        String body = """
                {"health": true, "exercise": true, "book": false, "software": false}
                """;
        for (UserEntity u : List.of(owner, owner, other)) {
            mockMvc.perform(post("/api/v1/daily/")
                            .with(authentication(authOf(u)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/daily").with(authentication(authOf(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/daily").with(authentication(authOf(other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("POST /daily/ zapisuje wpis z wybraną datą z przeszłości")
    void shouldSaveDailyForChosenDate() throws Exception {
        UserEntity u = userRepository.save(user("daily-date"));

        mockMvc.perform(post("/api/v1/daily/")
                        .with(authentication(authOf(u)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date": "15-03-2024", "health": true, "exercise": false, "book": false, "software": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("15-03-2024"));

        assertThat(dailyRepository.findAll())
                .singleElement()
                .satisfies(d -> assertThat(d.getDate()).isEqualTo(LocalDate.of(2024, 3, 15)));
    }

    @Test
    @DisplayName("POST /daily/ bez daty zapisuje dzisiejszą, a z datą w przyszłości zwraca 400")
    void shouldDefaultToTodayAndRejectFutureDate() throws Exception {
        UserEntity u = userRepository.save(user("daily-default"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        mockMvc.perform(post("/api/v1/daily/")
                        .with(authentication(authOf(u)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"health": false, "exercise": true, "book": false, "software": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(LocalDate.now().format(format)));

        String tomorrow = LocalDate.now().plusDays(1).format(format);
        mockMvc.perform(post("/api/v1/daily/")
                        .with(authentication(authOf(u)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\": \"" + tomorrow + "\", \"health\": true}"))
                .andExpect(status().isBadRequest());

        assertThat(dailyRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /daily/{id} usuwa własny wpis, a cudzy traktuje jak nieistniejący (404)")
    void shouldDeleteOnlyOwnDaily() throws Exception {
        UserEntity owner = userRepository.save(user("daily-del-owner"));
        UserEntity other = userRepository.save(user("daily-del-other"));
        DailyRecord own = dailyRepository.save(DailyRecord.from(
                new DailyRecordRequest(LocalDate.of(2024, 5, 1), true, false, false, false), owner));
        DailyRecord foreign = dailyRepository.save(DailyRecord.from(
                new DailyRecordRequest(LocalDate.of(2024, 5, 1), true, true, true, true), other));

        mockMvc.perform(delete("/api/v1/daily/" + foreign.getId()).with(authentication(authOf(owner))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/daily/" + own.getId()).with(authentication(authOf(owner))))
                .andExpect(status().isNoContent());

        assertThat(dailyRepository.findAll())
                .singleElement()
                .satisfies(d -> assertThat(d.getId()).isEqualTo(foreign.getId()));
    }

    private Authentication authOf(UserEntity user) {
        UserDetailsImpl principal = new UserDetailsImpl(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getPassword(), List.of(), true
        );
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
