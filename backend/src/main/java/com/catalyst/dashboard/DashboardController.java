package com.catalyst.dashboard;

import com.catalyst.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DashboardController {
    private final DashboardService dashboard;

    DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/api/dashboard")
    DashboardView dashboard(@AuthenticationPrincipal AuthenticatedUser me) {
        return dashboard.dashboard(me.id());
    }

    @GetMapping("/api/analytics")
    AnalyticsView analytics(@AuthenticationPrincipal AuthenticatedUser me) {
        return dashboard.analytics(me.id());
    }
}
