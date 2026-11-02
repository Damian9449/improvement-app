package com.improvement_app.workouts.repository;

import com.improvement_app.workouts.entity.TrainingEntity;
import com.improvement_app.workouts.entity.enums.ExerciseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingEntityRepository extends JpaRepository<TrainingEntity, Integer> {

    Page<TrainingEntity> findByUserId(Long userId, Pageable page);

    // EXISTS subquery avoids JOIN on the exercises collection — prevents HHH90003004 in-memory pagination warning.
    @Query(value = "SELECT t FROM TrainingEntity t WHERE t.user.id = :userId AND EXISTS (SELECT e FROM ExerciseEntity e WHERE e.training = t AND e.type = :exerciseType) ORDER BY t.date DESC",
           countQuery = "SELECT COUNT(t) FROM TrainingEntity t WHERE t.user.id = :userId AND EXISTS (SELECT e FROM ExerciseEntity e WHERE e.training = t AND e.type = :exerciseType)")
    Page<TrainingEntity> findByUserIdAndExercisesType(@Param("userId") Long userId, @Param("exerciseType") ExerciseType exerciseType, Pageable page);

    Optional<TrainingEntity> findTopByUserIdOrderByDateDesc(Long userId);

    // Bulk delete tylko treningów danego użytkownika — ćwiczenia i serie usuwa ON DELETE CASCADE w bazie
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TrainingEntity t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
