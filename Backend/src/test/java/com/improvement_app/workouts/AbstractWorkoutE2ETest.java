package com.improvement_app.workouts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.improvement_app.food.config.PermitAllSecurityConfig;
import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.googledrive.service.FilePathService;
import com.improvement_app.googledrive.service.GoogleDriveFileService;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.security.services.UserDetailsImpl;
import com.improvement_app.workouts.data.WorkoutTestDataFactory;
import com.improvement_app.workouts.entity.ExerciseNameEntity;
import com.improvement_app.workouts.entity.TrainingTemplateEntity;
import com.improvement_app.workouts.entity.enums.ExerciseName;
import com.improvement_app.workouts.repository.ExerciseEntityRepository;
import com.improvement_app.workouts.repository.ExerciseNameEntityRepository;
import com.improvement_app.workouts.repository.TrainingEntityRepository;
import com.improvement_app.workouts.repository.TrainingTemplateEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.improvement_app.workouts.data.WorkoutTestDataFactory.user;

/**
 * Base for workout E2E tests — MockMvc with auth post-processors,
 * real Postgres via Testcontainers, Drive/FilePath mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, PermitAllSecurityConfig.class})
@ActiveProfiles("test")
public abstract class AbstractWorkoutE2ETest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected TrainingEntityRepository trainingRepository;
    @Autowired protected ExerciseEntityRepository exerciseRepository;
    @Autowired protected TrainingTemplateEntityRepository templateRepository;
    @Autowired protected ExerciseNameEntityRepository exerciseNameRepository;
    @Autowired protected UserRepository userRepository;

    @MockitoBean protected GoogleDriveFileService googleDriveFileService;
    @MockitoBean protected FilePathService filePathService;

    @BeforeEach
    void cleanDb() {
        trainingRepository.deleteAll();
        templateRepository.deleteAll();
        exerciseNameRepository.deleteAll();
        userRepository.deleteAll();
        WorkoutTestDataFactory.reset();
    }

    protected UserEntity persistUser(String username) {
        return userRepository.save(user(username));
    }

    /**
     * Pass to .with(...) in MockMvc — wraps user as UserDetailsImpl (same as production).
     * UserDetailsImpl ma getId(), więc @AuthenticationPrincipal(expression="id") wyciąga id,
     * a AuditAwareImpl może go zrzutować na UserDetails dla @CreatedBy/@LastModifiedBy.
     */
    protected Authentication authOf(UserEntity user) {
        UserDetailsImpl principal = new UserDetailsImpl(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getPassword(), List.of(), true
        );
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    /**
     * Seeds a training template required by /trainingType/{type} and /training/{type}/maximum.
     * ExerciseNameEntity musi być zapisany osobno (ManyToMany bez cascade), więc nie używamy
     * factory.template() bezpośrednio — potrzebujemy persistowanych ExerciseNameEntity.
     */
    protected TrainingTemplateEntity persistTemplate(String name, ExerciseName... exercises) {
        TrainingTemplateEntity template = new TrainingTemplateEntity(name);
        for (ExerciseName ex : exercises) {
            ExerciseNameEntity saved = exerciseNameRepository.save(new ExerciseNameEntity(ex.getValue()));
            template.addExercise(saved);
        }
        return templateRepository.save(template);
    }

}
