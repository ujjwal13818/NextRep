package com.nextset.auth.security;

import com.nextset.auth.model.RefreshToken;
import com.nextset.auth.model.User;
import com.nextset.auth.service.RefreshTokenService;
import com.nextset.auth.service.UserService;
import com.nextset.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private static final String FRONTEND_REDIRECT_URL = "http://localhost:5173/auth/callback";

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil,
                                     RefreshTokenService refreshTokenService,
                                     UserService userService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        User user = userService.findOrCreateUser(googleId, email, name, pictureUrl);

        String accessToken = jwtUtil.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        String redirectUrl = FRONTEND_REDIRECT_URL
                + "?token=" + accessToken
                + "&refreshToken=" + refreshToken.getToken();

        response.sendRedirect(redirectUrl);
    }
}