package com.improvement_app.workouts.entity;

import com.improvement_app.audit.basic.AuditableEntity;
import com.improvement_app.workouts.entity.enums.ExerciseName;
import com.improvement_app.workouts.entity.enums.ExerciseProgress;
import com.improvement_app.workouts.entity.enums.ExerciseType;
import com.improvement_app.workouts.request.ExerciseRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercise", schema = "workout")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"training", "exerciseSets"})
@ToString(exclude = {"training", "exerciseSets"})
public class ExerciseEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exercise_seq")
    @SequenceGenerator(
            name = "exercise_seq",
            sequenceName = "exercise_id_seq",
            schema = "workout",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TrainingEntity training;

    @Enumerated(EnumType.STRING)
    private ExerciseName name;

    @Enumerated(EnumType.STRING)
    private ExerciseType type;

    @Enumerated(EnumType.STRING)
    private ExerciseProgress progress;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id")
    @BatchSize(size = 50)
    private List<ExerciseSetEntity> exerciseSets = new ArrayList<>();

    public ExerciseEntity(ExerciseName name, ExerciseType type, ExerciseProgress progress,
                          List<ExerciseSetEntity> exerciseSets) {
        this.name = name;
        this.type = type;
        this.progress = progress;
        this.exerciseSets = exerciseSets;
        for (ExerciseSetEntity exerciseSet : exerciseSets) {
            exerciseSet.setExercise(this);
        }
    }

    public ExerciseEntity(ExerciseName exerciseName) {
        this.name = exerciseName;
    }

    public static List<ExerciseEntity> create(List<ExerciseRequest> exerciseRequests) {
        List<ExerciseEntity> result = new ArrayList<>();

        for (ExerciseRequest request : exerciseRequests) {
            List<ExerciseSetEntity> sets = ExerciseSetEntity.fromString(request);
            ExerciseEntity exerciseEntity = new ExerciseEntity(
                    ExerciseName.fromValue(request.getName()),
                    ExerciseType.fromValue(request.getType()),
                    ExerciseProgress.fromValue(request.getProgress()),
                    sets
            );

            result.add(exerciseEntity);
        }

        return result;
    }

}