package assembly.general.api.controller;

import assembly.general.api.dto.ProfileResponse;
import assembly.general.api.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * the userId comes straight from the JWT .JwtAuthenticationFilter already
     * set it as the request's principal so Spring hands it back here without
     * needing to look anything up first
     */
    @GetMapping("/profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal UUID userId) {
        return authService.getProfile(userId);
    }
}