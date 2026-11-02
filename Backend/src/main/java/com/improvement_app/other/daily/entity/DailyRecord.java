package com.improvement_app.other.daily.entity;

import com.improvement_app.audit.basic.AuditableEntity;
import com.improvement_app.other.daily.request.DailyRecordRequest;
import com.improvement_app.security.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "daily_record", schema = "other")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"user"})
@ToString(exclude = {"user"})
public class DailyRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private LocalDate date;

    private boolean health;
    private boolean exercise;
    private boolean book;
    private boolean software;

    public static DailyRecord from(DailyRecordRequest request, UserEntity user) {
        DailyRecord dailyRecord = new DailyRecord();
        dailyRecord.user = user;
        dailyRecord.date = request.date() != null ? request.date() : LocalDate.now();
        dailyRecord.health = request.health();
        dailyRecord.exercise = request.exercise();
        dailyRecord.book = request.book();
        dailyRecord.software = request.software();
        return dailyRecord;
    }
}
