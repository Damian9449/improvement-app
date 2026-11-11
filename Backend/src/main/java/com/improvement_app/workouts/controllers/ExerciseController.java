package com.improvement_app.workouts.controllers;

import com.improvement_app.common.web.ApiVersions;
import com.improvement_app.util.ListResponse;
import com.improvement_app.workouts.dto.ExerciseSearchCriteria;
import com.improvement_app.workouts.response.ExerciseResponse;
import com.improvement_app.workouts.services.ExerciseService;
import com.improvement_app.workouts.validation.ValidTrainingType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Validated
@Tag(name = "Exercise API", description = "Controller to handle all operation on exercise database.")
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersions.V1 + "/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @Operation(
            summary = "Pobierz ćwiczenia po jednym z kryteriów",
            description = "Podaj dokładnie jeden z parametrów: date, name lub trainingName"
    )
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ListResponse<ExerciseResponse> searchExercises(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String trainingName,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {

        List<ExerciseResponse> result = exerciseService
                .findExercises(userId, new ExerciseSearchCriteria(date, name, trainingName))
                .stream()
                .map(ExerciseResponse::new)
                .toList();

        return ListResponse.of(result);
    }

    @Operation(summary = "Pobierz szablon ostatniego treningu", description = "Zwraca szablon treningu z ostatnimi wykonaniami ćwiczeń")
    @GetMapping(value = "/trainingType/{trainingType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ListResponse<ExerciseResponse> getTrainingFromTemplate(@PathVariable
                                                                  @ValidTrainingType String trainingType,
                                                                  @AuthenticationPrincipal(expression = "id") Long userId) {

        List<ExerciseResponse> exercises = exerciseService.generateTrainingFromTemplate(userId, trainingType)
                .stream()
                .map(ExerciseResponse::new)
                .toList();

        return ListResponse.of(exercises);
    }

    @Operation(summary = "Pobierz rekordowe ćwiczenia z treningu", description = "Zwraca ćwiczenia o maksymalnej wykonanej pracy (ATH) dla danego typu treningu")
    @GetMapping(value = "/trainingType/{trainingType}/maximum", produces = MediaType.APPLICATION_JSON_VALUE)
    public ListResponse<ExerciseResponse> getMaxTrainingExercises(@PathVariable
                                                                  @ValidTrainingType String trainingType,
                                                                  @AuthenticationPrincipal(expression = "id") Long userId) {

        List<ExerciseResponse> exercises = exerciseService.getATHExercise(userId, trainingType)
                .stream()
                .map(ExerciseResponse::new)
                .toList();

        return ListResponse.of(exercises);
    }

}
