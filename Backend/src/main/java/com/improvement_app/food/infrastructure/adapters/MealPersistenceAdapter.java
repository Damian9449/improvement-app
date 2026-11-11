package com.improvement_app.food.infrastructure.adapters;

import com.improvement_app.food.application.ports.out.MealPersistencePort;
import com.improvement_app.food.application.spec.MealRecipeSpecifications;
import com.improvement_app.food.domain.recipe.MealRecipe;
import com.improvement_app.food.domain.MealSearchCriteria;
import com.improvement_app.food.domain.MealSortCriteria;
import com.improvement_app.food.domain.enums.MealCategory;
import com.improvement_app.food.domain.enums.MealPopularity;
import com.improvement_app.food.domain.enums.MealType;
import com.improvement_app.food.infrastructure.repository.MealRecipeRepository;
import com.improvement_app.food.infrastructure.entity.meals.MealRecipeEntity;
import com.improvement_app.food.infrastructure.mappers.MealMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
@RequiredArgsConstructor
public class MealPersistenceAdapter implements MealPersistencePort {

    private final MealRecipeRepository mealRecipeRepository;
    private final MealMapper mealMapper;

    @Override
    public List<MealRecipe> findMeals(MealSearchCriteria searchCriteria, MealSortCriteria sortCriteria) {
        Specification<MealRecipeEntity> spec = buildSpecification(searchCriteria);
        Sort sort = buildSort(sortCriteria);

        List<MealRecipeEntity> entities = mealRecipeRepository.findAll(spec, sort);
        return mealMapper.toDomain(entities);
    }

    @Override
    public void deleteAll() {
        mealRecipeRepository.deleteAll();
    }

    @Override
    public List<MealRecipe> saveAll(List<MealRecipe> meals) {
        List<MealRecipeEntity> entities = mealMapper.toEntity(meals);

        List<MealRecipeEntity> saved = mealRecipeRepository.saveAll(entities);
        return mealMapper.toDomain(saved);
    }

    @Override
    public List<MealRecipe> findAll() {
        List<MealRecipeEntity> entities = mealRecipeRepository.findAll();
        return mealMapper.toDomain(entities);
    }

    private Specification<MealRecipeEntity> buildSpecification(MealSearchCriteria criteria) {
        // allOf pomija null, więc filtr "ALL" po prostu nie dodaje warunku
        return Specification.allOf(
                criteria.category() != MealCategory.ALL ?
                        MealRecipeSpecifications.hasCategory(criteria.category()) : null,
                criteria.type() != MealType.ALL ?
                        MealRecipeSpecifications.hasType(criteria.type()) : null,
                criteria.popularity() != MealPopularity.ALL ?
                        MealRecipeSpecifications.hasPopularity(criteria.popularity()) : null,
                MealRecipeSpecifications.hasNameContaining(criteria.nameContains()));
    }

    private Sort buildSort(MealSortCriteria sortCriteria) {
        Sort.Direction direction = sortCriteria.ascending() ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortCriteria.sortBy()) {
            case "name" -> Sort.by(direction, "name");
            case "popularity" -> Sort.by(direction, "popularity");
            case "category" -> Sort.by(direction, "category");
            case "type" -> Sort.by(direction, "type");
            default -> Sort.by(direction, "name");
        };
    }
}
