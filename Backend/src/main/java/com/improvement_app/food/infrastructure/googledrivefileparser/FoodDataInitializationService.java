package com.improvement_app.food.infrastructure.googledrivefileparser;

import com.improvement_app.food.application.ports.out.InitializerPort;
import com.improvement_app.food.domain.exceptions.DataInitializationException;
import com.improvement_app.food.infrastructure.entity.meals.ProductEntity;
import com.improvement_app.food.infrastructure.repository.MealRecipeRepository;
import com.improvement_app.food.infrastructure.repository.ProductRepository;
import com.improvement_app.food.infrastructure.googledrivefileparser.initializers.MealInitializer;
import com.improvement_app.food.infrastructure.googledrivefileparser.initializers.ProductInitializer;
import com.improvement_app.food.infrastructure.googledrivefileparser.initializers.SweetsInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.improvement_app.common.websocket.UserLogNotifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodDataInitializationService implements InitializerPort {

    private final ProductInitializer productInitializer;
    private final MealInitializer mealInitializer;
    private final SweetsInitializer sweetsInitializer;
    private final ProductRepository productRepository;
    private final MealRecipeRepository mealRecipeRepository;
    private final UserLogNotifier userLogNotifier;

    private static final String INITIALIZATION_START_MESSAGE = "Starting food data initialization";
    private static final String INITIALIZATION_SUCCESS_MESSAGE = "Food data initialization completed successfully";
    private static final String INITIALIZATION_ERROR_MESSAGE = "Food data initialization failed";

    @Override
//    @Transactional
    public void initializeData(String username) {
        log.info(INITIALIZATION_START_MESSAGE);
        userLogNotifier.send(username, INITIALIZATION_START_MESSAGE);

        try {
            clearExistingData();

            List<ProductEntity> products = productInitializer.initializeProducts(username);
            mealInitializer.initializeMeals(products, username);
            sweetsInitializer.initializeSweets(username);

            log.info(INITIALIZATION_SUCCESS_MESSAGE);
            userLogNotifier.send(username, INITIALIZATION_SUCCESS_MESSAGE);

        } catch (Exception e) {
            log.error(INITIALIZATION_ERROR_MESSAGE, e);
            userLogNotifier.send(username, INITIALIZATION_ERROR_MESSAGE + ": " + e.getMessage());
            throw new DataInitializationException("Failed to initialize food data", e);
        }
    }

    private void clearExistingData() {
        log.info("Clearing existing data from database");
        mealRecipeRepository.deleteAll();
        productRepository.deleteAll();
    }
}