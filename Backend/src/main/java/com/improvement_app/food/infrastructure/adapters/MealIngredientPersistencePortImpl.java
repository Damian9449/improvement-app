package com.improvement_app.food.infrastructure.adapters;

import com.improvement_app.food.application.ports.out.MealIngredientPersistencePort;
import com.improvement_app.food.domain.recipe.MealIngredient;
import com.improvement_app.food.infrastructure.entity.meals.MealIngredientEntity;
import com.improvement_app.food.infrastructure.repository.MealIngredientRepository;
import com.improvement_app.food.infrastructure.mappers.MealIngredientMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Transactional
@RequiredArgsConstructor
public class MealIngredientPersistencePortImpl implements MealIngredientPersistencePort {

    private final MealIngredientRepository mealIngredientRepository;
    private final MealIngredientMapper mealIngredientMapper;


    @Override
    public List<MealIngredient> getMealIngredients(Long mealId) {
        List<MealIngredientEntity> ingredientsEntity = mealIngredientRepository.findByMealRecipeEntityId(mealId);
        return mealIngredientMapper.toDomainList(ingredientsEntity);
    }

}
