package com.improvement_app.workouts.controllers;

import com.improvement_app.common.web.ApiVersions;
import com.improvement_app.workouts.services.InitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersions.V1 + "/drive")
public class GoogleDriveTrainingController {

    private final InitializationService initializationService;

    @GetMapping(value = "/initApplication", produces = MediaType.APPLICATION_JSON_VALUE)
    public void initApplication(@AuthenticationPrincipal(expression = "id") Long userId) {
        initializationService.initApplicationTemplates();
        initializationService.initApplicationTrainings(userId);
    }

}
