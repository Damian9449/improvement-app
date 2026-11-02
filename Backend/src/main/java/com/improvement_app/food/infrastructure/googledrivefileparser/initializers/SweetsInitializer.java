package com.improvement_app.food.infrastructure.googledrivefileparser.initializers;

import com.improvement_app.food.infrastructure.entity.meals.MealRecipeEntity;
import com.improvement_app.food.infrastructure.repository.MealRecipeRepository;
import com.improvement_app.googledrive.service.FileDownloadService;
import com.improvement_app.food.infrastructure.googledrivefileparser.parsers.SweetsParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.improvement_app.common.websocket.UserLogNotifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.improvement_app.food.FoodModuleVariables.SWEETS_SHEET_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class SweetsInitializer {

    private final FileDownloadService fileDownloadService;
    private final SweetsParser sweetsParser;
    private final MealRecipeRepository mealRecipeRepository;
    private final UserLogNotifier userLogNotifier;

    private static final String SWEETS_PROCESSING_MESSAGE = "Processing sweets from file: {}";
    private static final String SWEETS_SAVED_MESSAGE = "Successfully saved {} sweets to database";

    public void initializeSweets(String username) throws IOException {
        log.info(SWEETS_PROCESSING_MESSAGE, SWEETS_SHEET_NAME);
        userLogNotifier.send(username, "Loading sweets...");

        try {
            File sweetsFile = fileDownloadService.downloadFile(SWEETS_SHEET_NAME);
            List<MealRecipeEntity> sweets = sweetsParser.parseExcelProductsFile(sweetsFile);

            validateSweets(sweets);
            mealRecipeRepository.saveAll(sweets);

            log.info(SWEETS_SAVED_MESSAGE, sweets.size());
            userLogNotifier.send(username,
                    String.format("Loaded %d sweets", sweets.size()));

        } catch (IOException e) {
            log.error("Failed to initialize sweets from file: {}", SWEETS_SHEET_NAME, e);
            throw new IOException("Sweets initialization failed", e);
        }
    }

    private void validateSweets(List<MealRecipeEntity> sweets) {
        if (sweets == null || sweets.isEmpty()) {
            log.warn("No sweets found in file: {}", SWEETS_SHEET_NAME);
            return;
        }

        long invalidSweets = sweets.stream()
                .filter(sweet -> sweet.getName() == null || sweet.getName().trim().isEmpty())
                .count();

        if (invalidSweets > 0) {
            log.warn("Found {} sweets with invalid names", invalidSweets);
        }
    }
}