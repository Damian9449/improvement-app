package com.improvement_app.other.daily.repository;

import com.improvement_app.other.daily.entity.DailyRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyRepository extends JpaRepository<DailyRecord, Long> {

    Page<DailyRecord> findByUserId(Long userId, Pageable pageable);

    Optional<DailyRecord> findByIdAndUserId(Long id, Long userId);

}
