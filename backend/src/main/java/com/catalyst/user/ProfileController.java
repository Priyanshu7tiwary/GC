package com.catalyst.user;

import com.catalyst.security.AuthenticatedUser;
import com.catalyst.user.dto.ProfileResponse;
import com.catalyst.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
class ProfileController {
    private final ProfileService profiles;

    ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    ProfileResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return profiles.get(me.id());
    }

    /** Update name, target percentile, and target exam year / date. */
    @PatchMapping
    ProfileResponse update(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return profiles.update(me.id(), req);
    }
}
