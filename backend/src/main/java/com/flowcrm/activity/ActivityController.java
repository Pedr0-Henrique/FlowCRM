package com.flowcrm.activity;

import com.flowcrm.activity.dto.ActivityRequest;
import com.flowcrm.activity.dto.ActivityResponse;
import com.flowcrm.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(activityService.findAll(
                user.getCompanyId(),
                PageRequest.of(page, Math.min(size, 100), Sort.by("occurredAt").descending())
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES', 'USER')")
    public ResponseEntity<ActivityResponse> create(
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(activityService.create(user.getCompanyId(), user.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        activityService.delete(id, user.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
