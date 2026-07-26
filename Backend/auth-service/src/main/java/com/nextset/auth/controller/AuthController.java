package com.nextset.auth.controller;

import com.nextset.auth.model.RefreshToken;
import com.nextset.auth.service.RefreshTokenService;
import com.nextset.auth.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public AuthController(RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String requestRefreshToken = body.get("refreshToken");

        return refreshTokenService.verify(requestRefreshToken)
                .map(rt -> {
                    String newAccessToken = jwtUtil.generateToken(rt.getUser().getEmail());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(rt.getUser());

                    return ResponseEntity.ok(Map.of(
                            "token", newAccessToken,
                            "refreshToken", newRefreshToken.getToken()
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        refreshTokenService.revoke(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}