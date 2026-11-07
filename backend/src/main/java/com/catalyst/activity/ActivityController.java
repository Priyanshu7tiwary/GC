package com.catalyst.activity;

import com.catalyst.activity.dto.ActivityView;
import com.catalyst.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
@Validated
class ActivityController {
    private final ActivityService activity;

    ActivityController(ActivityService activity) {
        this.activity = activity;
    }

    @GetMapping
    ActivityView get(@AuthenticationPrincipal AuthenticatedUser me,
                     @RequestParam(defaultValue = "112") @Min(7) @Max(366) int days) {
        return activity.view(me.id(), days);
    }
}
