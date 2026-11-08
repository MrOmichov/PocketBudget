package org.mromichov.pocket_budget.security;

import org.mromichov.pocket_budget.users.BudgetUserDetails;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthSessionController {
    @GetMapping("/api/auth/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal BudgetUserDetails principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CurrentUserResponse(principal.getId(), principal.getUsername()));
    }

    public record CsrfResponse(String headerName, String token) {}
    public record CurrentUserResponse(long id, String username) {}
}
