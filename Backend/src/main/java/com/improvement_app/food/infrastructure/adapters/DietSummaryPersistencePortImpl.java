package com.improvement_app.food.infrastructure.adapters;

import com.improvement_app.food.application.ports.out.DietSummaryPersistencePort;
import com.improvement_app.food.domain.summary.DietSummary;
import com.improvement_app.food.infrastructure.entity.summary.DietSummaryEntity;
import com.improvement_app.food.infrastructure.repository.DietSummaryRepository;
import com.improvement_app.food.infrastructure.mappers.DietSummaryMapper;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Slf4j
@Configuration
@Transactional
@RequiredArgsConstructor
public class DietSummaryPersistencePortImpl implements DietSummaryPersistencePort {

    private final DietSummaryRepository dietSummaryRepository;
    private final DietSummaryMapper dietSummaryMapper;

    private final UserRepository userRepository;

    @Override
    public DietSummary save(Long userId, DietSummary dietSummary) {
        log.debug("Saving diet summary {}, for user: {}", dietSummary, userId);

        DietSummaryEntity dietSummaryEntity = dietSummaryMapper.toEntity(dietSummary);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        dietSummaryEntity.setUser(userEntity);
        DietSummaryEntity savedDietSummary = dietSummaryRepository.save(dietSummaryEntity);

        log.debug("Diet summary saved with id: {} for user: {}", savedDietSummary.getId(), userId);

        return dietSummaryMapper.toDomain(savedDietSummary);
    }

    @Override
    public Page<DietSummary> findAll(Long userId, Pageable pageable) {
        log.debug("Fetching diet summaries for user: {} with pageable: {}", userId, pageable);

        Page<DietSummaryEntity> dietSummaries = dietSummaryRepository.findByUserId(userId, pageable);

        log.debug("Found {} diet summaries for user: {}", dietSummaries.getTotalElements(), userId);

        return dietSummaries.map(dietSummaryMapper::toDomain);
    }

    @Override
    public void deleteById(Long userId, Long id) {
        log.debug("Deleting diet summary with id: {} for user: {}", id, userId);

        int deletedCount = dietSummaryRepository.deleteByIdAndUserId(id, userId);

        if (deletedCount == 0) {
            log.warn("No diet summary found with id: {} for user: {} - nothing deleted", id, userId);
            throw new EntityNotFoundException("Diet summary not found or access denied");
        }

        log.debug("Diet summary with id: {} deleted for user: {}", id, userId);
    }

    @Override
    public Optional<DietSummary> findById(Long userId, Long id) {
        log.debug("Fetching diet summary with id: {}", id);

        Optional<DietSummaryEntity> dietSummary = dietSummaryRepository.findByIdAndUserId(id, userId);

        return dietSummary.map(dietSummaryMapper::toDomain);
    }

}
