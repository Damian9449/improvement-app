package com.improvement_app.other.daily.service;

import com.improvement_app.other.daily.entity.DailyRecord;
import com.improvement_app.other.daily.repository.DailyRepository;
import com.improvement_app.other.daily.request.DailyRecordRequest;
import com.improvement_app.other.daily.response.DailyRecordResponse;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class DailyService {

    private final DailyRepository dailyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<DailyRecordResponse> getDailyList(Long userId, Pageable pageable) {
        return dailyRepository.findByUserId(userId, pageable)
                .map(DailyRecordResponse::from);
    }

    @Transactional
    public DailyRecordResponse addDaily(Long userId, DailyRecordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        DailyRecord saved = dailyRepository.save(DailyRecord.from(request, user));
        return DailyRecordResponse.from(saved);
    }

    // Cudzy wpis traktujemy jak nieistniejący — nie zdradzamy, że takie id jest w bazie
    @Transactional
    public void deleteDaily(Long userId, Long id) {
        DailyRecord dailyRecord = dailyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Daily record not found: " + id));
        dailyRepository.delete(dailyRecord);
    }

}
