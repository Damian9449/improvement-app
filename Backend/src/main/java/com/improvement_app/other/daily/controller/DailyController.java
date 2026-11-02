package com.improvement_app.other.daily.controller;

import com.improvement_app.common.web.ApiVersions;
import com.improvement_app.other.daily.request.DailyRecordRequest;
import com.improvement_app.other.daily.response.DailyRecordResponse;
import com.improvement_app.other.daily.service.DailyService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping(ApiVersions.V1 + "/daily")
public class DailyController {

    private final DailyService dailyService;

    @PostMapping("/")
    public ResponseEntity<DailyRecordResponse> addDaily(
            @Valid @RequestBody DailyRecordRequest request,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        DailyRecordResponse savedDaily = dailyService.addDaily(userId, request);
        return ResponseEntity.ok(savedDaily);
    }

    @GetMapping
    public ResponseEntity<Page<DailyRecordResponse>> getDaily(
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {

        Page<DailyRecordResponse> allDailies = dailyService.getDailyList(userId, pageable);

        return ResponseEntity.ok(allDailies);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDaily(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        dailyService.deleteDaily(userId, id);
        return ResponseEntity.noContent().build();
    }

}
