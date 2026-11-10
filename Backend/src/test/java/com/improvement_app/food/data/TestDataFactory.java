package com.improvement_app.food.data;

import com.improvement_app.food.domain.enums.*;
import com.improvement_app.food.infrastructure.repository.DietSummaryRepository;
import com.improvement_app.food.infrastructure.repository.MealRecipeRepository;
import com.improvement_app.food.infrastructure.repository.ProductRepository;
import com.improvement_app.food.infrastructure.entity.meals.MealIngredientEntity;
import com.improvement_app.food.infrastructure.entity.meals.MealRecipeEntity;
import com.improvement_app.food.infrastructure.entity.meals.ProductEntity;
import com.improvement_app.food.infrastructure.entity.summary.DietSummaryEntity;
import com.improvement_app.food.infrastructure.entity.summary.DailyMealEntity;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.*;

/**
 * Klasa do generowania kompletnych danych testowych z zachowaniem relacji kluczy obcych.
 * Używa Datafaker z stałym seedem dla powtarzalności testów.
 */
public class TestDataFactory {

    private static Faker faker = new Faker(Locale.of("pl"), new Random(42L));

    /**
     * Resetuje Faker do początkowego stanu z tym samym seedem
     */
    public static void resetFaker() {
        faker = new Faker(Locale.of("pl"), new Random(42L));
    }

    // ============================================
    // BUILDERY - zwracają buildera z losowymi wartościami
    // ============================================

    public static ProductEntity.ProductEntityBuilder productBuilder() {
        return ProductEntity.builder()
                .id(faker.number().randomNumber())
                .name(faker.food().ingredient())
                .kcal(faker.number().randomDouble(2, 50, 500))
                .protein(faker.number().randomDouble(2, 1, 35))
                .carbohydrates(faker.number().randomDouble(2, 0, 60))
                .fat(faker.number().randomDouble(2, 0, 25))
                .amount(100.0)
                .unit(faker.options().option(Unit.class))
                .productCategory(faker.options().option(ProductCategory.class));
    }

    public static MealRecipeEntity.MealRecipeEntityBuilder mealRecipeBuilder() {
        return MealRecipeEntity.builder()
                .id(faker.number().randomNumber())
                .name(faker.food().dish())
                .kcal(faker.number().randomDouble(2, 200, 1000))
                .protein(faker.number().randomDouble(2, 10, 60))
                .carbohydrates(faker.number().randomDouble(2, 20, 100))
                .fat(faker.number().randomDouble(2, 5, 40))
                .portionAmount(faker.number().randomDouble(2, 200, 500))
                .url(faker.internet().url())
                .type(faker.options().option(MealType.class))
                .category(faker.options().option(MealCategory.class))
                .popularity(faker.options().option(MealPopularity.class))
                .recipe(List.of(
                        "Krok 1: " + faker.lorem().sentence(8),
                        "Krok 2: " + faker.lorem().sentence(10),
                        "Krok 3: " + faker.lorem().sentence(7)
                ))
                .ingredients(new ArrayList<>());
    }

    public static MealIngredientEntity.MealIngredientEntityBuilder mealIngredientBuilder() {
        return MealIngredientEntity.builder()
                .name(faker.food().ingredient())
                .amount(faker.number().randomDouble(2, 20, 300))
                .unit(faker.options().option(Unit.class));
    }

    public static DietSummaryEntity.DietSummaryEntityBuilder dietSummaryBuilder() {
        return DietSummaryEntity.builder()
                .kcal(faker.number().randomDouble(2, 1500, 2500))
                .protein(faker.number().randomDouble(2, 80, 180))
                .carbohydrates(faker.number().randomDouble(2, 150, 350))
                .fat(faker.number().randomDouble(2, 50, 130))
                .date(LocalDate.now().minusDays(faker.number().numberBetween(0, 30)))
                .meals(new ArrayList<>());
    }

    public static UserEntity.UserEntityBuilder userBuilder() {
        final String firstName = faker.name().firstName();
        final String lastName = faker.name().lastName();

        return UserEntity.builder()
                .id(faker.number().randomNumber())
                .name(firstName)
                .surname(lastName)
                .username(faker.name().username())
                .password("password")
                .email(String.format("%s.%s.%s@example.test",
                        firstName.toLowerCase(), lastName.toLowerCase(), UUID.randomUUID().toString().substring(0, 6)));

    }

    // ============================================
    // GENERATOR - kompleksowe dane testowe
    // ============================================

    /**
     * Generator kompletnych danych testowych z wszystkimi relacjami.
     * Automatycznie zapisuje wszystko w odpowiedniej kolejności.
     */
    public static class TestDataGenerator {
        private final ProductRepository productRepository;
        private final MealRecipeRepository mealRecipeRepository;
        private final DietSummaryRepository dietSummaryRepository;
        private final UserRepository userRepository;

        private final List<ProductEntity> generatedProducts = new ArrayList<>();
        private final List<MealRecipeEntity> generatedMeals = new ArrayList<>();
        private final List<DietSummaryEntity> generatedSummaries = new ArrayList<>();

        private UserEntity user;

        public TestDataGenerator(
                ProductRepository productRepository,
                MealRecipeRepository mealRecipeRepository,
                DietSummaryRepository dietSummaryRepository,
                UserRepository userRepository) {
            this.productRepository = productRepository;
            this.mealRecipeRepository = mealRecipeRepository;
            this.dietSummaryRepository = dietSummaryRepository;
            this.userRepository = userRepository;
        }

        /**
         * Generuje i zapisuje usera
         */
        public TestDataGenerator withUser() {
            UserEntity generatedUser = userBuilder().build();
            this.user =  userRepository.save(generatedUser);
            return this;
        }


        /**
         * Generuje i zapisuje produkty
         */
        public TestDataGenerator withProducts(int count) {
            for (int i = 0; i < count; i++) {
                ProductEntity product = productBuilder().build();
                ProductEntity saved = productRepository.save(product);
                generatedProducts.add(saved);
            }
            return this;
        }

        /**
         * Generuje i zapisuje produkty z konkretną kategorią
         */
        public TestDataGenerator withProducts(int count, ProductCategory category) {
            for (int i = 0; i < count; i++) {
                ProductEntity product = productBuilder()
                        .productCategory(category)
                        .build();
                ProductEntity saved = productRepository.save(product);
                generatedProducts.add(saved);
            }
            return this;
        }

        /**
         * Generuje i zapisuje przepisy na posiłki z losowymi składnikami
         */
        public TestDataGenerator withMealRecipes(int mealCount, int ingredientsPerMeal) {
            if (generatedProducts.size() < ingredientsPerMeal) {
                throw new IllegalStateException(
                        "Nie ma wystarczająco produktów! Wygeneruj najpierw produkty używając withProducts()"
                );
            }

            for (int i = 0; i < mealCount; i++) {
                MealRecipeEntity meal = mealRecipeBuilder().build();

                List<MealIngredientEntity> ingredients = new ArrayList<>();
                for (int j = 0; j < ingredientsPerMeal; j++) {
                    ProductEntity randomProduct = generatedProducts.get(
                            faker.number().numberBetween(0, generatedProducts.size())
                    );

                    MealIngredientEntity ingredient = mealIngredientBuilder()
                            .productEntity(randomProduct)
                            .mealRecipeEntity(meal)
                            .name(randomProduct.getName())
                            .amount(faker.number().randomDouble(2, 50, 300))
                            .unit(randomProduct.getUnit())
                            .build();

                    ingredients.add(ingredient);
                }

                meal.setIngredients(ingredients);
                MealRecipeEntity saved = mealRecipeRepository.save(meal);
                generatedMeals.add(saved);
            }

            return this;
        }

        /**
         * Generuje przepisy z określonym typem posiłku
         */
        public TestDataGenerator withMealRecipes(int mealCount, int ingredientsPerMeal, MealType type) {
            if (generatedProducts.size() < ingredientsPerMeal) {
                throw new IllegalStateException(
                        "Nie ma wystarczająco produktów! Wygeneruj najpierw produkty używając withProducts()"
                );
            }

            for (int i = 0; i < mealCount; i++) {
                MealRecipeEntity meal = mealRecipeBuilder()
                        .type(type)
                        .build();

                List<MealIngredientEntity> ingredients = new ArrayList<>();
                for (int j = 0; j < ingredientsPerMeal; j++) {
                    ProductEntity randomProduct = generatedProducts.get(
                            faker.number().numberBetween(0, generatedProducts.size())
                    );

                    MealIngredientEntity ingredient = mealIngredientBuilder()
                            .productEntity(randomProduct)
                            .mealRecipeEntity(meal)
                            .name(randomProduct.getName())
                            .amount(faker.number().randomDouble(2, 50, 300))
                            .unit(randomProduct.getUnit())
                            .build();

                    ingredients.add(ingredient);
                }

                meal.setIngredients(ingredients);
                MealRecipeEntity saved = mealRecipeRepository.save(meal);
                generatedMeals.add(saved);
            }

            return this;
        }

        public TestDataGenerator withDietSummaries(int daysBack) {
            if (generatedMeals.isEmpty()) {
                throw new IllegalStateException(
                    "Nie ma przepisów! Wygeneruj najpierw przepisy używając withMealRecipes()"
                );
            }

            if (this.user == null) {
                throw new IllegalStateException(
                        "Nie ma usera! Wygeneruj najpierw usera używając withUser()"
                );
            }

            for (int i = 0; i < daysBack; i++) {
                LocalDate date = LocalDate.now().minusDays(i);

                // Losowa liczba zjedzonych posiłków (2-5 dziennie)
                int mealsPerDay = faker.number().numberBetween(2, 6);
                List<DailyMealEntity> eatenMeals = new ArrayList<>();

                double totalKcal = 0;
                double totalProtein = 0;
                double totalCarbs = 0;
                double totalFat = 0;

                for (int j = 0; j < mealsPerDay; j++) {
                    MealRecipeEntity randomMeal = generatedMeals.get(
                            faker.number().numberBetween(0, generatedMeals.size())
                    );

                    double portionMultiplier = faker.number().randomDouble(2, 1, 2);

                    DailyMealEntity eatenMeal = new DailyMealEntity(
                            randomMeal.getId(),
                            randomMeal.getName(),
                            randomMeal.getKcal() * portionMultiplier,
                            randomMeal.getProtein() * portionMultiplier,
                            randomMeal.getCarbohydrates() * portionMultiplier,
                            randomMeal.getFat() * portionMultiplier,
                            portionMultiplier,
                            List.of()
//                            randomMeal.getIngredients()
                    );

                    eatenMeals.add(eatenMeal);

                    totalKcal += eatenMeal.getCachedKcal();
                    totalProtein += eatenMeal.getCachedProtein();
                    totalCarbs += eatenMeal.getCachedCarbohydrates();
                    totalFat += eatenMeal.getCachedFat();
                }

                DietSummaryEntity summary = dietSummaryBuilder()
                        .user(this.user)
                        .date(date)
                        .meals(eatenMeals)
                        .kcal(totalKcal)
                        .protein(totalProtein)
                        .carbohydrates(totalCarbs)
                        .fat(totalFat)
                        .build();

                DietSummaryEntity saved = dietSummaryRepository.save(summary);
                generatedSummaries.add(saved);
            }

            return this;
        }

        public List<ProductEntity> getProducts() {
            return new ArrayList<>(generatedProducts);
        }

        public List<MealRecipeEntity> getMealRecipes() {
            return new ArrayList<>(generatedMeals);
        }

        public List<DietSummaryEntity> getDietSummaries() {
            return new ArrayList<>(generatedSummaries);
        }

        public void cleanUp() {
            dietSummaryRepository.deleteAll(generatedSummaries);
            mealRecipeRepository.deleteAll(generatedMeals);
            productRepository.deleteAll(generatedProducts);
            userRepository.deleteAll();

            generatedSummaries.clear();
            generatedMeals.clear();
            generatedProducts.clear();
        }
    }

    public static TestDataGenerator generator(
            ProductRepository productRepository,
            MealRecipeRepository mealRecipeRepository,
            DietSummaryRepository dietSummaryRepository,
            UserRepository userRepository) {
        return new TestDataGenerator(productRepository, mealRecipeRepository, dietSummaryRepository, userRepository);
    }
}
