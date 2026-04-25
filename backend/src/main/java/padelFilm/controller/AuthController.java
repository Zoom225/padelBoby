package com.padelPlay.controller;

import com.padelPlay.dto.request.LoginRequest;
import com.padelPlay.dto.response.LoginResponse;
import com.padelPlay.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for administrator authentication. " +
        "Members do NOT authenticate here — they are identified only by their matricule " +
        "and do not need a token to access member-facing endpoints. " +
        "This authentication flow is reserved exclusively for administrators (GLOBAL or SITE) " +
        "who need access to protected management endpoints. " +
        "Authentication process: " +
        "1. The administrator sends their email and password via POST /api/auth/login. " +
        "2. The server validates the credentials and returns a JWT token valid for 24 hours. " +
        "3. The administrator includes the token in subsequent requests using the Authorization header: " +
        "Authorization: Bearer <token>. " +
        "4. The token encodes the administrator email and role (GLOBAL or SITE). " +
        "Security note: the same error message is returned whether the email does not exist " +
        "or the password is incorrect, which helps prevent user enumeration attacks.")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Administrator login",
            description = "Authenticates an administrator using their email and password. " +
                    "Returns a JWT token to be used in the Authorization header for all protected endpoints. " +
                    "The token is valid for 24 hours. " +
                    "The response also includes the administrator profile information and role: " +
                    "- GLOBAL: can view and manage all sites. " +
                    "- SITE: can manage only their assigned site. " +
                    "Important: this endpoint is public and does not require any existing token. " +
                    "Do NOT use this endpoint for member authentication — members use their matricule directly."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT token returned",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials — wrong email or password. " +
                    "The same message is returned for both cases to prevent user enumeration.",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error — email format invalid or fields missing",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
