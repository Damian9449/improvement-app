package com.improvement_app.workouts.services;

import com.improvement_app.googledrive.service.FilePathService;
import com.improvement_app.googledrive.service.GoogleDriveFileService;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.workouts.converters.TrainingTypeConverter;
import com.improvement_app.workouts.entity.TrainingEntity;
import com.improvement_app.workouts.entity.enums.ExerciseType;
import com.improvement_app.workouts.helpers.DriveFilesHelper;
import com.improvement_app.workouts.repository.TrainingEntityRepository;
import com.improvement_app.workouts.request.ExerciseRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

import static com.improvement_app.workouts.TrainingModuleVariables.DRIVE_TRAININGS_FOLDER_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingEntityRepository trainingRepository;
    private final UserRepository userRepository;

    private final GoogleDriveFileService googleDriveFileService;
    private final FilePathService filePathService;

    @Transactional
    public void recreateTraining(Long userId, List<TrainingEntity> trainings) {
        int deleted = trainingRepository.deleteAllByUserId(userId);
        log.info("Usunięto {} starych treningów użytkownika {}", deleted, userId);
        trainingRepository.saveAll(trainings);
        log.info("Zapisano wszystkie {} treningi użytkownika {}", trainings.size(), userId);
    }

    @Transactional
    public List<TrainingEntity> save(List<TrainingEntity> trainings) {
        return trainingRepository.saveAll(trainings);
    }

    @Transactional(readOnly = true)
    public Page<String> getAllTrainingNames(Long userId, Pageable page) {
        return trainingRepository.findByUserId(userId, page)
                .map(TrainingEntity::getName);
    }

    @Transactional
    public TrainingEntity addTraining(Long userId, List<ExerciseRequest> exerciseRequest) {
        if (exerciseRequest == null || exerciseRequest.isEmpty()) {
            throw new IllegalArgumentException("Lista ćwiczeń nie może być pusta");
        }

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        TrainingEntity latestTraining = trainingRepository.findTopByUserIdOrderByDateDesc(userId)
                .orElse(null);
        ExerciseType type = ExerciseType.fromValue(exerciseRequest.get(0).getType());
        String trainingName = DriveFilesHelper.generateFileName(type, latestTraining);

        TrainingEntity training = TrainingEntity.from(exerciseRequest);
        training.setName(trainingName);
        training.setUser(userEntity);

        // Google Drive is the source of truth — upload first.
        // If Drive fails, we throw and nothing is persisted (consistent state).
        // If DB fails after a successful Drive upload, initApplicationTrainings()
        // can restore the DB from Drive at any time.
        uploadTrainingToDrive(exerciseRequest, trainingName);

        return trainingRepository.save(training);
    }

    @Transactional
    private void uploadTrainingToDrive(List<ExerciseRequest> exercises, String trainingName) {
        String excelFileLocation = filePathService.getExcelPath(trainingName);
        DriveFilesHelper.createExcelFile(exercises, excelFileLocation);
        File file = new File(excelFileLocation);
        googleDriveFileService.uploadFile(DRIVE_TRAININGS_FOLDER_NAME, file, trainingName);
    }

    @Transactional(readOnly = true)
    public Page<TrainingEntity> getLastTrainings(Long userId, String type, Pageable page) {
        final String dbExerciseType = TrainingTypeConverter.toExerciseType(type);
        final ExerciseType exerciseType = ExerciseType.valueOf(dbExerciseType);

        Page<TrainingEntity> trainings = trainingRepository.findByUserIdAndExercisesType(userId, exerciseType, page);

        // @BatchSize(size=50) on both exercises and exerciseSets batches these into
        // 2 SQL queries total instead of N×M individual selects.
        trainings.getContent().forEach(training -> {
            Hibernate.initialize(training.getExercises());
            training.getExercises().forEach(exercise ->
                    Hibernate.initialize(exercise.getExerciseSets())
            );
        });

        return trainings;
    }

}
