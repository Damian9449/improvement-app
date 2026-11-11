package com.improvement_app.workouts.controllers;

import com.improvement_app.common.web.ApiVersions;
import com.improvement_app.workouts.response.ChartPoint;
import com.improvement_app.workouts.services.StatisticService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import java.util.List;


@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersions.V1 + "/exercises/statistic/")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping(value = "{exerciseName}/{chartType}/{beginDate}/{endDate}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChartPoint>> getTrainingStatisticData(
                                            @PathVariable
                                            @NotBlank(message = "Exercise name is required")
                                            String exerciseName,

                                            @PathVariable
                                            @Pattern(regexp = "^(Capacity|Weight)$", message = "Chart type must be 'Capacity' or 'Weight'")
                                            String chartType,

                                            @PathVariable
                                            @Pattern(regexp = "^\\d{2}-\\d{2}-\\d{4}$", message = "Begin date must be in format dd-MM-yyyy")
                                            String beginDate,

                                            @PathVariable
                                            @Pattern(regexp = "^\\d{2}-\\d{2}-\\d{4}$", message = "End date must be in format dd-MM-yyyy")
                                            String endDate,

                                            @AuthenticationPrincipal(expression = "id") Long userId)
    {

        List<ChartPoint> dataToChart
                = statisticService.generateStatisticChartData(userId, exerciseName, chartType, beginDate, endDate);

        return ResponseEntity.ok(dataToChart);
    }

}
