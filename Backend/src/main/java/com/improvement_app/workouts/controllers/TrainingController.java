package com.improvement_app.workouts.controllers;

import com.improvement_app.common.web.ApiVersions;
import com.improvement_app.workouts.entity.TrainingEntity;
import com.improvement_app.workouts.request.ExerciseRequest;
import com.improvement_app.workouts.response.TrainingDayResponse;
import com.improvement_app.workouts.services.TrainingService;
import com.improvement_app.workouts.validation.ValidTrainingType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@Tag(name = "Training API", description = "Controller to handle all operation on training database.")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersions.V1 + "/trainings")
public class TrainingController {

    private final TrainingService trainingService;

    @Operation(summary = "Pobierz nazwy wszystkich treningów", description = "Zwraca stronicowaną listę nazw treningów")
    @GetMapping("/names")
    public Page<String> getTrainingNames(@PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC)
                                         Pageable pageable,
                                         @AuthenticationPrincipal(expression = "id") Long userId) {

        return trainingService.getAllTrainingNames(userId, pageable);
    }

    @Operation(summary = "Pobierz ostatnie treningi danego typu", description = "Zwraca stronicowaną listę ostatnich treningów danego typu")
    @GetMapping(value = "/{trainingType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<TrainingDayResponse> getLastTrainingsType(@PathVariable
                                                          @ValidTrainingType String trainingType,
                                                          @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC)
                                                          Pageable pageable,
                                                          @AuthenticationPrincipal(expression = "id") Long userId) {

        return trainingService.getLastTrainings(userId, trainingType, pageable)
                .map(TrainingDayResponse::from);
    }

    @Operation(summary = "Dodaj nowy trening", description = "Zapisuje trening w bazie i przesyła kopię na Google Drive")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TrainingDayResponse> addTraining(@Valid @RequestBody List<ExerciseRequest> exercises,
                                                           @AuthenticationPrincipal(expression = "id") Long userId) {

        TrainingEntity training = trainingService.addTraining(userId, exercises);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TrainingDayResponse.from(training));
    }

}
