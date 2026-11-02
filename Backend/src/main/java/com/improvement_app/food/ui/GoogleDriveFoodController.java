package com.improvement_app.food.ui;

import com.improvement_app.common.web.ApiVersions;
import com.improvement_app.food.application.InitializationDataService;
import com.improvement_app.food.application.ports.in.InitializationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersions.V1 + "/drive")
public class GoogleDriveFoodController {

    private final InitializationUseCase initializationUseCase;

    @GetMapping("/initFoodModule")
    public void initProducts(@AuthenticationPrincipal(expression = "username") String username) throws IOException {
        log.info("Usuwam i dodaje nowe produkty i posiłki");
        initializationUseCase.initializeData(username);
    }

}
