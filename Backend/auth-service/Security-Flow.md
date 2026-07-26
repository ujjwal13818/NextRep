# NextSet Auth Service — Complete Google OAuth2 + JWT + Refresh Token Flow

This document explains **everything** that happens from the moment a user clicks "Sign in with Google" to being a fully authenticated user with a session that can silently renew itself for days — in the exact chronological order it actually happens, with every file's code and a plain-English explanation of each piece.

---

## Part 1 — The Cast of Files

Before walking through the timeline, here's every file involved and its one-line job:

| File | Package | Job |
|---|---|---|
| `SecurityConfig.java` | `config` | The rulebook — defines which URLs are public, which need auth, wires everything together |
| `JwtAuthFilter.java` | `security` | Runs on every request — checks for a valid JWT, tells Spring Security who's asking |
| `OAuth2LoginSuccessHandler.java` | `security` | Runs once per login — creates/finds the user, issues tokens, redirects to frontend |
| `JwtUtil.java` | `util` | The crypto engine — signs and verifies JWTs |
| `User.java` | `model` | JPA entity — one row per person who's ever logged in |
| `UserRepository.java` | `repository` | Database access for `User` — no code needed, Spring generates it |
| `RefreshToken.java` | `model` | JPA entity — one row per active "stay logged in" session |
| `RefreshTokenRepository.java` | `repository` | Database access for `RefreshToken` |
| `UserService.java` | `service` | Find-or-create logic for users |
| `RefreshTokenService.java` | `service` | Create/verify/revoke logic for refresh tokens |
| `AuthController.java` | `controller` | The two HTTP endpoints: `/auth/refresh` and `/auth/logout` |
| `AuthCallback.jsx` | frontend | Catches the redirect, stores tokens, sends the user into the app |

Nothing here is Spring Boot magic you have to take on faith — every one of these is either a file you wrote, or (in the case of the repositories) an interface Spring fills in automatically based on method names, which we'll explain when we get there.

---

## Part 2 — A Quick Primer on JPA (Since Half These Files Depend On It)

**JPA (Jakarta Persistence API)** is a specification — a set of rules — for mapping Java objects to database tables, without you writing raw SQL for basic operations. **Hibernate** is the actual library that implements this specification underneath Spring Boot.

Three annotations you'll see constantly:

- **`@Entity`** — "this Java class corresponds to a database table." Put on `User` and `RefreshToken`.
- **`@Table(name = "...")`** — explicitly names the table (otherwise Hibernate guesses from the class name).
- **`@Id`** / **`@GeneratedValue`** — marks the primary key and tells Hibernate how to generate new ones (we use `GenerationType.UUID` — a random unique ID per row, generated in Java before insert).
- **`@Column`** — customizes how a Java field maps to a table column (name, nullability, length, precision).
- **`@ManyToOne`** / **`@JoinColumn`** — defines a foreign key relationship. `RefreshToken` has a `@ManyToOne` to `User`, meaning many refresh tokens can point to one user, and the actual foreign key column in the `refresh_tokens` table is named `user_id`.

**The other half of JPA — repositories:**

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleId(String googleId);
}
```

You never implement this interface. Spring Data JPA reads the method name (`findByGoogleId`) at startup, matches `GoogleId` against the `googleId` field on `User` via reflection, and **generates a dynamic proxy class at runtime** that runs the equivalent of `SELECT * FROM users WHERE google_id = ?`. This is called **query derivation** — the method name itself *is* the query definition. `JpaRepository<User, UUID>` also comes pre-loaded with `save()`, `findById()`, `findAll()`, `deleteById()`, etc., implemented inside Spring Data's own library code.

**`ddl-auto=update`** (in your `application.properties`) tells Hibernate to automatically create/alter tables to match your `@Entity` classes on startup — convenient for development, something you'd replace with proper migrations (Flyway/Liquibase) before production, since `update` can make unexpected schema changes silently.

---

## Part 3 — The Full Chronological Walkthrough

### Step 0 — Before Any of This: Google Cloud Console Setup

You registered an OAuth client in **console.cloud.google.com**, which gave you a **Client ID** and **Client Secret**, and you told Google exactly one URL it's allowed to redirect back to:
```
http://localhost:8081/login/oauth2/code/google
```
This pre-registration is a core OAuth2 security mechanism — Google will only hand back an authorization code to a URL you've explicitly whitelisted for that client ID, preventing an attacker from redirecting the code somewhere malicious.

These values live in your `.env` and get read into `application.properties`:
```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```
The word `google` right after `.registration.` is called the **registrationId** — Spring Security uses this exact string to auto-generate two URLs you never had to build yourself: `/oauth2/authorization/google` and `/login/oauth2/code/google`. Both show up later in this timeline.

---

### Step 1 — User Clicks "Sign in with Google"

**File: `Auth.jsx` (frontend)**
```jsx
const handleGoogleLogin = () => {
  window.location.href = 'http://localhost:8081/oauth2/authorization/google';
};
```

This is a **full page navigation**, not an API call — the browser leaves your React app entirely and requests this URL directly from your Spring Boot backend. Because it's a full navigation and not a `fetch`/`axios` call, browser CORS rules don't apply here at all.

---

### Step 2 — Spring Security Redirects to Google

The moment that request lands on your backend, `SecurityConfig`'s `oauth2Login(...)` configuration (explained fully in Step 8) intercepts it — this specific URL pattern (`/oauth2/authorization/google`) is entirely auto-handled by Spring Security's OAuth2 client machinery. No controller method exists for this in your code; Spring Security owns this route internally, based purely on your `application.properties` client registration.

Spring redirects the browser to Google's actual login page, attaching:
- Your `client-id`
- The `scope` (`openid,profile,email`) — telling Google what data you're requesting permission for
- The `redirect_uri` (`http://localhost:8081/login/oauth2/code/google`) — where to send the user back afterward

---

### Step 3 — User Logs Into Google, Grants Consent

This entire step happens on **Google's servers**, not yours. The user sees Google's actual login UI, picks their account, and approves the permissions your `scope` requested. Nothing in your codebase runs during this step.

---

### Step 4 — Google Redirects Back to Your Callback URL

Google redirects the browser to the exact `redirect_uri` from Step 2, appending a temporary **authorization code**:
```
http://localhost:8081/login/oauth2/code/google?code=4/0AY0e-g7...&state=xyz
```

This URL — `/login/oauth2/code/google` — is the second auto-generated Spring Security endpoint (pattern: `/login/oauth2/code/{registrationId}`). You never wrote a `@GetMapping` for it; Spring Security's `OAuth2LoginAuthenticationFilter` intercepts requests matching this exact pattern automatically.

---

### Step 5 — Spring Exchanges the Code for the User's Profile (Behind the Scenes)

Still inside that same auto-handled filter, Spring Security makes a **separate, server-to-server** HTTPS call to Google's token endpoint, trading the authorization code (plus your client secret, proving it's really your app asking) for:
- An access token **from Google** (used only to call Google's own APIs — completely separate from the JWT your app will issue later)
- The user's profile data: `sub` (Google's permanent unique user ID), `email`, `name`, `picture`

This is the point where Spring Security has fully verified "yes, this really is a real Google user, and here's their info" — and it wraps all of that into an `OAuth2User` object, then fires the next step.

---

### Step 6 — `OAuth2LoginSuccessHandler` Runs (Your Code, First Time)

This is the **first place your own custom code executes** in the entire flow.

```java
package com.nextset.auth.security;

import com.nextset.auth.model.RefreshToken;
import com.nextset.auth.model.User;
import com.nextset.auth.service.RefreshTokenService;
import com.nextset.auth.service.UserService;
import com.nextset.auth.util.JwtUtil;
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
    private static final String FRONTEND_REDIRECT_URL = "http://localhost:3000/auth/callback";

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
```

**What's happening line by line:**
- `implements AuthenticationSuccessHandler` — this is Spring Security's interface for "run this after any successful login." Wired in via `SecurityConfig`'s `.successHandler(...)` (Step 8).
- `.getAttribute("sub")` etc. — pulls specific fields out of the profile data Google returned in Step 5. `sub` is Google's own permanent, stable ID for this user — more reliable than email, since (in theory) a user could change their email on Google's side.
- `userService.findOrCreateUser(...)` — **this is the database write.** Explained fully in Step 7.
- `jwtUtil.generateToken(...)` — creates the short-lived (15 min) access token. Explained in Step 9.
- `refreshTokenService.createRefreshToken(...)` — creates the long-lived (7 day) refresh token, **saved to Postgres**. Explained in Step 10.
- `response.sendRedirect(...)` — sends the browser to your React app, with both tokens riding along as query parameters.

---

### Step 7 — `UserService` Finds or Creates the User Row

```java
package com.nextset.auth.service;

import com.nextset.auth.model.User;
import com.nextset.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateUser(String googleId, String email, String name, String pictureUrl) {
        return userRepository.findByGoogleId(googleId)
            .map(existingUser -> {
                boolean changed = false;

                if (name != null && !name.equals(existingUser.getName())) {
                    existingUser.setName(name);
                    changed = true;
                }
                if (pictureUrl != null && !pictureUrl.equals(existingUser.getPictureUrl())) {
                    existingUser.setPictureUrl(pictureUrl);
                    changed = true;
                }

                return changed ? userRepository.save(existingUser) : existingUser;
            })
            .orElseGet(() -> {
                User newUser = new User(email, name, pictureUrl, googleId);
                return userRepository.save(newUser);
            });
    }
}
```

**What's happening:**
- `userRepository.findByGoogleId(googleId)` — first login ever for this person → returns empty. Every login after that → finds the existing row.
- `.map(...)` — **if found**, checks whether their name/photo changed on Google's side since last time, updates only if needed (avoids pointless writes on every single login), then returns the (possibly updated) user.
- `.orElseGet(...)` — **if not found**, this is a brand new signup: builds a new `User` object and `.save()`s it — this is the actual `INSERT INTO users (...)` moment.

**The `User` entity itself:**
```java
package com.nextset.auth.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;

    @Column(name = "bodyweight_kg", precision = 5, scale = 2)
    private BigDecimal bodyweightKg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {}

    public User(String email, String name, String pictureUrl, String googleId) {
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.googleId = googleId;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // getters/setters omitted for brevity
}
```

- `@PrePersist` / `@PreUpdate` — Hibernate lifecycle hooks. These methods run automatically right before an insert or update — you never manually set `createdAt`/`updatedAt` anywhere in your service code.
- `bodyweightKg` is nullable — it's set later during onboarding, not at signup, which is why it's fine for a fresh user to have `null` here.

**Why `googleId` is looked up instead of `email`:** email is a reasonable secondary unique constraint, but `sub` (stored as `googleId`) is Google's own permanent identifier — the more correct field to treat as "this is definitely the same person" across repeated logins.

---

### Step 8 — Meanwhile, `SecurityConfig` Is What Made All of This Possible

This file doesn't run *during* the login — it configured everything *at application startup*, and every step above only worked because these rules already existed.

```java
package com.nextset.auth.config;

import com.nextset.auth.security.JwtAuthFilter;
import com.nextset.auth.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/**", "/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- **`.csrf(csrf -> csrf.disable())`** — CSRF protection defends against session-cookie-based attacks. You're using stateless JWTs instead of sessions, so this specific protection doesn't apply to your architecture — disabling it is standard for JWT APIs.
- **`.sessionManagement(... STATELESS)`** — tells Spring: never create an HTTP session, never remember anything between requests server-side. Every single request must prove who it is on its own — this is the foundational decision that makes JWT auth possible and horizontally scalable.
- **`.requestMatchers("/oauth2/**", "/login/**", "/auth/**").permitAll()`** — these three patterns must stay open to unauthenticated users: `/oauth2/**` and `/login/**` are the two auto-generated endpoints from Steps 2 and 4 (a user isn't authenticated yet while they're in the middle of *becoming* authenticated), and `/auth/**` covers your own `/auth/refresh` and `/auth/logout` endpoints — which also must be reachable without a currently-valid access token, since renewing an expired one is the whole point.
- **`.anyRequest().authenticated()`** — everything else requires a valid JWT.
- **`.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))`** — this single line activates the entire Steps 2–6 flow. Everything about talking to Google is auto-configured by the `spring-boot-starter-oauth2-client` dependency plus your `application.properties`; the only thing you're customizing is what happens on success.
- **`.addFilterBefore(jwtAuthFilter, ...)`** — inserts `JwtAuthFilter` (Step 12) into the chain, positioned to run before the standard username/password filter.

---

### Step 9 — `JwtUtil` Creates the Access Token

```java
package com.nextset.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

**What a JWT actually is:** three base64-encoded segments separated by dots — `header.payload.signature`. The payload holds your claims (`subject`=email, `issuedAt`, `expiration`) in plain readable JSON once decoded — **a JWT is not encrypted, only signed**. Anyone can read its contents; what they *can't* do is modify it undetected, because the signature (generated with your secret key) would no longer match.

- **`@Value("${jwt.secret}")`** — resolves through the chain: `.env` file → OS environment variable (loaded via `spring-dotenv`) → `application.properties`'s `jwt.secret=${JWT_SECRET}` → this annotation. Three layers, each only depending on the name of the layer below it — meaning you could swap where the secret comes from (a secrets manager, a different profile) without ever touching this Java file.
- **`Keys.hmacShaKeyFor(...)`** — turns your plain string secret into a proper cryptographic key object. HMAC is symmetric — the same key both signs new tokens and verifies incoming ones.
- **`generateToken`** — this is called once per login (Step 6) and once per refresh (Step 14). Sets a 15-minute expiration (`jwt.expiration-ms=900000`).
- **`extractEmail`** / **`isTokenValid`** — used later by `JwtAuthFilter` (Step 12) on every subsequent request to figure out who's asking and whether their token is still good.
- **`extractAllClaims`** — this is where signature verification actually happens. `.verifyWith(getSigningKey())` means: if the token wasn't signed with this exact key, or if a single character of the payload was altered after signing, this line throws an exception rather than returning fake-valid claims.

---

### Step 10 — `RefreshTokenService` Creates the Refresh Token (The DB Write That Enables Staying Logged In)

```java
package com.nextset.auth.service;

import com.nextset.auth.model.RefreshToken;
import com.nextset.auth.model.User;
import com.nextset.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken(
            UUID.randomUUID().toString(),
            user,
            Instant.now().plusMillis(refreshExpirationMs)
        );

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> verify(String token) {
        return refreshTokenRepository.findByToken(token)
            .filter(rt -> !rt.isRevoked())
            .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
    }

    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
```

**Why a refresh token exists at all — the core trade-off:**

| | Access Token (JWT) | Refresh Token |
|---|---|---|
| Lifespan | 15 minutes | 7 days |
| Checked on | Every API request | Only when access token expires |
| Stored server-side? | No (stateless) | Yes — in Postgres |
| Revocable early? | No | Yes |

JWTs are **stateless** — your server never looks them up anywhere, it just re-verifies the signature. This makes them fast but means they **can't be cancelled early** once issued. Refresh tokens are deliberately the opposite: just a random string, stored as a literal row in your database, which means you *can* delete it or flip `revoked = true` whenever you need to force a logout. Keep the frequent thing (per-request checks) fast and stateless; keep the rare thing (long-term sessions) revocable.

- **`refreshTokenRepository.deleteByUser(user)`** — runs *before* creating the new one. This enforces **one active refresh token per user at any time** — a security pattern called **rotation**. Every time a new refresh token is issued (login, or later renewal), the previous one is destroyed, limiting how long a stolen refresh token remains useful.
- **`UUID.randomUUID().toString()`** — the token itself is just a random opaque string, unlike the JWT — no embedded meaning, since it's looked up directly in the database anyway.
- **`verify(token)`** — three conditions must all pass: the token exists, it isn't revoked, and it hasn't passed its expiry. Used by `/auth/refresh` (Step 14).
- **`revoke(token)`** — flips the `revoked` flag. Used by `/auth/logout` (Step 15) — this is what makes logout actually mean something server-side, unlike just clearing `localStorage` on the frontend.

**The `RefreshToken` entity:**
```java
package com.nextset.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    // constructors, getters/setters omitted for brevity
}
```

- **`@ManyToOne(fetch = FetchType.LAZY)`** — many refresh tokens can reference one user (over time, as tokens rotate); `LAZY` means the associated `User` object is only actually fetched from the DB when you call `.getUser()`, not automatically loaded every time a `RefreshToken` is retrieved.
- **`@JoinColumn(name = "user_id")`** — this is the real foreign key column in the `refresh_tokens` table, replacing what was originally a loose `userEmail` string — a proper relational constraint instead of a string match.

---

### Step 11 — Browser Redirects to Your React App, `AuthCallback.jsx` Catches It

Back on the frontend, `response.sendRedirect(...)` from Step 6 lands the browser on:
```
http://localhost:3000/auth/callback?token=eyJhbGci...&refreshToken=a1b2c3d4...
```

```jsx
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

function AuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('processing');

  useEffect(() => {
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');

    if (token && refreshToken) {
      localStorage.setItem('accessToken', token);
      localStorage.setItem('refreshToken', refreshToken);
      window.history.replaceState({}, document.title, '/auth/callback');
      navigate('/dashboard', { replace: true });
    } else {
      setStatus('error');
      setTimeout(() => navigate('/auth', { replace: true }), 2000);
    }
  }, [searchParams, navigate]);

  return status === 'processing'
    ? <p>Logging you in...</p>
    : <p>Something went wrong. Redirecting you back...</p>;
}

export default AuthCallback;
```

- **`useSearchParams()`** — React Router's hook for reading query string values — this is how the component gets `token` and `refreshToken` out of the URL.
- **`localStorage.setItem(...)`** — persists both tokens in the browser, surviving page refreshes/tab closes (unlike React state, which resets).
- **`window.history.replaceState(...)`** — scrubs the tokens out of the visible URL and browser history immediately after reading them, so they don't linger somewhere a screenshot or shared link could expose them.
- **`navigate('/dashboard', { replace: true })`** — sends the user into the actual app. `replace: true` means this doesn't add a new browser-history entry, so clicking "back" doesn't bounce them back to the callback URL.

**At this exact moment, the user is fully logged in.** Everything from here forward is about staying logged in.

---

### Step 12 — Every Subsequent Request: `JwtAuthFilter`

From here on, every API call the frontend makes includes the access token:
```
Authorization: Bearer eyJhbGci...
```

```java
package com.nextset.auth.security;

import com.nextset.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String email = jwtUtil.extractEmail(token);

                if (email != null && jwtUtil.isTokenValid(token)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                logger.warn("JWT validation failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- **`extends OncePerRequestFilter`** — a Spring base class guaranteeing this logic runs exactly once per request.
- **`.substring(7)`** — strips the literal 7 characters `"Bearer "`, leaving just the raw token.
- **`jwtUtil.extractEmail(token)`** + **`isTokenValid(token)`** — this is where `JwtUtil` (Step 9) gets reused, now for verification instead of creation.
- **`SecurityContextHolder.getContext().setAuthentication(authToken)`** — this line is what makes `.anyRequest().authenticated()` (back in `SecurityConfig`, Step 8) pass. Without it, Spring Security has no idea who's making the request, even with a technically valid JWT in the header.
- **`catch (Exception e)`** — if the token is expired or tampered with, this swallows the exception **without blocking the request here**. The request continues with no authentication set, which means it'll simply fail the `.authenticated()` check downstream, and Spring Security returns a 401 on its own.

---

### Step 13 — 15 Minutes Later: Access Token Expires

The next API call with the now-expired JWT gets a `401 Unauthorized` back — `isTokenValid` returns false because `isTokenExpired` is checking the `expiration` claim against right-now.

On the frontend, this is where an **axios interceptor** (or equivalent `fetch` wrapper) watches for 401 responses and automatically triggers the next step — silently, without the user noticing anything happened.

---

### Step 14 — Frontend Calls `/auth/refresh`

```java
package com.nextset.auth.controller;

import com.nextset.auth.model.RefreshToken;
import com.nextset.auth.service.RefreshTokenService;
import com.nextset.auth.util.JwtUtil;
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
        refreshTokenService.revoke(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
```

**Why this endpoint is reachable without a valid JWT** (it's covered by `/auth/**` in the `permitAll()` list from Step 8): a client calling this has, by definition, an *expired* access token — that's the entire reason they're calling it. The refresh token is the credential being checked here instead.

- **`refreshTokenService.verify(...)`** — the three-condition check from Step 10 (exists, not revoked, not expired).
- **If valid:** issues a **new** access token AND a **new** refresh token — this is rotation in action. The old refresh token was already deleted the moment `createRefreshToken` ran again inside this same call chain.
- **If invalid:** returns 401 — the frontend catches this and redirects the user back to `/auth` to log in via Google again from scratch.

This cycle repeats silently roughly every 15 minutes for as long as the user is active, and only forces an actual re-login once every 7 days — or immediately, if they explicitly log out.

---

### Step 15 — Logout (When It Happens)

```java
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
    refreshTokenService.revoke(body.get("refreshToken"));
    return ResponseEntity.ok(Map.of("message", "Logged out"));
}
```

This marks the refresh token `revoked = true` in Postgres — permanently useless from this point forward, even if someone had captured it beforehand. (The already-issued 15-minute access token would still technically work until its own natural expiry — a small, accepted residual window, which is exactly why keeping that token short-lived matters.)

---

## Part 4 — The Whole Thing as One Diagram

```
[User clicks "Sign in with Google"]
         │
         ▼
GET /oauth2/authorization/google  ─────────► Spring Security (auto) redirects to Google
         │
         ▼
[User logs into Google, grants consent]
         │
         ▼
Google redirects to:
GET /login/oauth2/code/google?code=...  ───► Spring Security (auto) exchanges code for profile
         │
         ▼
OAuth2LoginSuccessHandler runs:
  1. UserService.findOrCreateUser()  ──────► INSERT or UPDATE into `users` table
  2. JwtUtil.generateToken()  ──────────────► 15-min access token created
  3. RefreshTokenService.createRefreshToken() ► INSERT into `refresh_tokens` table
  4. sendRedirect to React app with both tokens
         │
         ▼
AuthCallback.jsx stores both tokens in localStorage, navigates to /dashboard
         │
         ▼
[User is now logged in — every API call sends Authorization: Bearer <access token>]
         │
         ▼
JwtAuthFilter validates the JWT on every request  ──► sets SecurityContext, or leaves it unauthenticated
         │
         ▼
   ... 15 minutes pass ...
         │
         ▼
Access token expires → next request gets 401
         │
         ▼
Frontend calls POST /auth/refresh with the refresh token
         │
         ├── Valid ──► New access + refresh token issued, old refresh token deleted (rotation)
         │
         └── Invalid/expired/revoked ──► 401 → frontend redirects to /auth (log in again)
```

---

## Part 5 — Known Gaps, Honestly Stated

1. **No refresh token reuse detection** — if a rotated (already-deleted) refresh token is presented again, a hardened setup would treat that as a signal of theft and revoke all of that user's sessions. Currently it just fails normally.
2. **Single refresh token per user, not per device** — logging in on a second device invalidates the first device's session, since `deleteByUser` wipes any existing token for that user. Multi-device support would need a `device_id` field and more targeted deletion logic.
3. **No IP/user-agent binding** — a stolen refresh token currently works from anywhere.
4. **CORS isn't configured yet** — fine for the redirect-based login flow (full page navigations aren't subject to CORS), but will matter once the frontend makes `fetch`/`axios` calls to `/auth/refresh` and other endpoints from `localhost:3000` to `localhost:8081` — a `CorsConfigurationSource` bean will be needed.

These are reasonable to defer at this stage of the project, but worth knowing they're the natural next things to harden.