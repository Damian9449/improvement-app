package com.improvement_app.workouts;

import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.workouts.entity.TrainingEntity;
import com.improvement_app.workouts.entity.enums.ExerciseName;
import com.improvement_app.workouts.entity.enums.ExerciseType;
import com.improvement_app.workouts.services.TrainingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static com.improvement_app.workouts.data.WorkoutTestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ponowna inicjalizacja treningów (import z Google Drive) podmienia tylko treningi użytkownika,
 * który ją uruchomił — treningi innych użytkowników zostają nietknięte.
 */
class TrainingRecreateE2ETest extends AbstractWorkoutE2ETest {

    @Autowired private TrainingService trainingService;

    @Test
    @DisplayName("recreateTraining usuwa i zapisuje treningi tylko danego użytkownika")
    void shouldReplaceOnlyCurrentUsersTrainings() {
        UserEntity a = persistUser("init-a");
        UserEntity b = persistUser("init-b");

        trainingRepository.save(training(a, LocalDate.of(2024, 1, 1),
                exercise(ExerciseName.POMPKI, ExerciseType.SILOWY_A, set(10.0, 50.0))));
        trainingRepository.save(training(b, LocalDate.of(2024, 1, 2),
                exercise(ExerciseName.DIPY, ExerciseType.SILOWY_A, set(8.0, 20.0), set(8.0, 25.0))));

        TrainingEntity imported = training(a, LocalDate.of(2024, 2, 1),
                exercise(ExerciseName.POMPKI, ExerciseType.SILOWY_A, set(12.0, 55.0)));

        trainingService.recreateTraining(a.getId(), List.of(imported));

        List<TrainingEntity> trainingsA = trainingRepository.findByUserId(a.getId(), Pageable.unpaged()).getContent();
        List<TrainingEntity> trainingsB = trainingRepository.findByUserId(b.getId(), Pageable.unpaged()).getContent();

        assertThat(trainingsA).extracting(TrainingEntity::getDate).containsExactly(LocalDate.of(2024, 2, 1));
        assertThat(trainingsB).extracting(TrainingEntity::getDate).containsExactly(LocalDate.of(2024, 1, 2));

        // Ćwiczenia i serie A usunięte kaskadowo, B nietknięte (1 ćwiczenie nowe A + 1 ćwiczenie B)
        assertThat(exerciseRepository.count()).isEqualTo(2);
    }
}
