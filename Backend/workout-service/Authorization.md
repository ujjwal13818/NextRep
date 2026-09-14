# NextSet Authentication & Authorization — Full Explainer

This document assumes zero prior knowledge. It explains every concept touched during the auth implementation, then walks through exactly what was built and why.

---

## 1. The Basics: What Are HTTP Headers?

Every HTTP request/response is made of two parts:
- **Headers** — metadata about the request (not the actual data you're sending). Think of it like the envelope of a letter: sender, recipient, delivery instructions — not the letter's contents.
- **Body** — the actual data payload (e.g., JSON you're sending to create a workout set).

Example of headers on a request:
```
GET /api/workouts/days HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

Each line before the blank line is a header. `Authorization` is a **standard, reserved header name** specifically meant for carrying credentials (tokens, API keys, etc.). This is why JWTs go in headers, not the request body — it's the correct, standardized location, and frameworks like Spring Security are built to look there automatically.

---

## 2. What Is a JWT (JSON Web Token)?

A JWT is a compact, self-contained way to represent "who this user is" as a signed string. It has 3 parts, separated by dots:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIn0.4f8a...
   HEADER            .    PAYLOAD          .  SIGNATURE
```

- **Header** — says which algorithm was used to sign it (e.g., `HS256`)
- **Payload (claims)** — the actual data: user ID (`sub` = subject), expiry time, etc. This part is **base64-encoded, not encrypted** — anyone can decode and read it, but they can't *modify* it without invalidating the signature.
- **Signature** — a cryptographic proof that the token wasn't tampered with. Computed using a secret key that only the server(s) know.

**Why this matters:** a JWT is **self-verifying**. Any service holding the same secret key can check "is this signature valid?" without ever calling back to the Auth service. This is what makes JWT-based auth *stateless* — no database lookup, no session table, no cross-service network call needed just to check if a user is logged in.

---

## 3. Symmetric Signing: HS256

We chose **HS256** (HMAC-SHA256), which uses **one shared secret key** for both:
- **Signing** the token (done by Auth module, when a user logs in)
- **Verifying** the token (done by Auth module AND Workout module, on every incoming request)

The alternative (RS256) uses a private/public key pair — more secure at scale (only Auth module *could* forge tokens) but more complex. For a 2–3 service portfolio project, HS256 with a shared secret is the right level of complexity.

**Practically, this meant:**
- Generate one random secret: `openssl rand -base64 32`
- Store it as `JWT_SECRET` in **both** Auth module's and Workout module's environment/config
- Both modules read it via `application.yml`:
  ```yaml
  jwt:
    secret: ${JWT_SECRET}
  ```

---

## 4. The Login Flow (Google OAuth2)

1. User clicks "Login with Google" on the frontend
2. Frontend redirects to Auth module's OAuth endpoint: `/oauth2/authorization/google`
3. Auth module redirects the browser to Google's actual login page
4. User logs in with Google, Google redirects back to Auth module with an authorization code
5. Auth module exchanges that code with Google for the user's identity (email, name, etc.)
6. Auth module **generates a JWT** (signed with the shared HS256 secret), containing the user's ID as the `subject` claim
7. Auth module redirects the browser to the frontend's callback page, with the token attached as a URL query parameter:
   ```
   http://localhost:5174/auth/callback?token=eyJ...&refreshToken=eyJ...
   ```

This redirect (step 7) is an **HTTP 302 "Found"** response — a temporary redirect. The server sends back a `Location` header telling the browser exactly where to go next, and the browser automatically makes a new request there.

---

## 5. `AuthCallback` — What This Component Does

This is a React component that runs the moment the browser lands on `/auth/callback` (right after step 7 above). Its job:

1. Read `token` and `refreshToken` from the URL's query parameters
2. Save them into `localStorage` (the browser's persistent key-value storage, scoped per **origin** — see Section 7 for why this became a problem)
3. Clean the URL (remove the token from the visible address bar/history, so it doesn't linger somewhere a screenshot or browser history could leak it)
4. Redirect the user onward to the actual app (`/exercises`)

---

## 6. Every Request After Login: How the Token Gets Used

Once logged in, every API call the frontend makes must prove "this user is authenticated." This is done by attaching the token in the `Authorization` header:

```javascript
fetch("http://localhost:8080/api/workouts/days", {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

`Bearer` is just a convention meaning "here is a token, treat it as proof of identity — whoever *bears* this token is authenticated."

---

## 7. The Cross-Origin `localStorage` Problem

**Origin** = protocol + domain + port. `http://localhost:5173` and `http://localhost:5174` are **different origins**, even though they're both "localhost" — different port = different origin, by browser security rules.

`localStorage` is *sandboxed per origin*. Data saved by JavaScript running on port 5174 is **completely invisible** to JavaScript running on port 5173. This is a deliberate browser security boundary (so one website can't read another website's stored data), not a bug.

**Why this bit us:** authUi and workoutui were built as two separate frontend apps, running on two separate ports. Login happened on one origin, saved the token there — but the `/exercises` page lived on a *different* origin, and had no access to that saved token. Result: "No auth token found" even though login had technically succeeded.

**The fix applied:** pass the token through the URL when redirecting between the two origins, and have the receiving app re-save it into *its own* `localStorage` on arrival. This works, but it's fragile — it has to be repeated for every future frontend module.

**The longer-term recommendation:** merge the frontend into a single React app (single origin), so this problem disappears entirely. This does **not** affect the backend microservices — Auth, Workout, AI Recommendation, and Analytics remain fully separate Spring Boot services regardless of whether the frontend is one app or many. Microservices is a backend architecture decision; this cross-origin issue was purely a frontend one.

---

## 8. Spring Security — How the Backend Checks Every Request

Spring Security intercepts every incoming HTTP request through a chain of **filters** before it ever reaches your `@RestController` methods. Think of filters as checkpoints in a row — each one inspects the request and either lets it continue or blocks it.

### `JwtAuthFilter` — the custom filter we wrote

This filter runs once per request (`OncePerRequestFilter`) and does exactly this:
1. Look for the `Authorization` header
2. If missing → let the request continue with no identity attached (later filters will reject it if the endpoint requires auth)
3. If present → strip off `"Bearer "`, take the raw token string
4. Verify the token's signature using the shared secret key
5. If valid → extract the `userId` (from the token's `subject` claim) and store it in Spring Security's `SecurityContextHolder` — this is how, later, inside a controller, you can ask "who is making this request?" without re-parsing the token yourself
6. If invalid/expired → clear any security context, request continues unauthenticated

### `SecurityConfig` — the rules this filter's output gets checked against

This class defines:
- Which endpoints require authentication vs. which are public
- That the app is **stateless** (no server-side session tracking — everything relies on the JWT each time)
- CORS rules (see Section 9)
- Registers `JwtAuthFilter` to run **before** Spring's built-in username/password filter, since we're not using traditional login forms

Each microservice (Auth, Workout, and later AI Recommendation, Analytics) needs **its own copy** of this — they don't share a runtime, so each has to independently know how to verify tokens and which of its own endpoints are protected.

---

## 9. CORS and the OPTIONS Preflight Problem

**CORS (Cross-Origin Resource Sharing)** is the browser's rule that says: "a script running on origin A cannot freely call an API on origin B, unless B explicitly says it's okay."

Before the browser sends certain "risky" cross-origin requests (like ones carrying custom headers, e.g. `Authorization`), it first sends an automatic **preflight check** — an `OPTIONS` request — basically asking the server "if I were to send this real request, would you accept it?" Only if the server responds correctly does the browser then send the actual `GET`/`POST` request.

**What went wrong:** Spring Security was treating this `OPTIONS` preflight request like any other protected request. Since preflight requests never carry the `Authorization` header (browsers don't attach custom headers to preflight checks), Spring Security saw "no valid token" and responded with a login redirect (302 to Google's OAuth page). But a 302 is not a response the browser accepts for a preflight — it's not one of the valid "yes, this cross-origin call is fine" answers. So the browser aborted the whole thing, and the real request (`GET /api/workouts/days`) never even got sent.

**The fix:**
1. Explicitly tell Spring Security to let **all** `OPTIONS` requests through without requiring authentication:
   ```java
   .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
   ```
2. Add an actual CORS configuration bean, listing exactly which origins (`localhost:5173`, `localhost:5174`), methods, and headers are allowed:
   ```java
   config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
   config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
   ```

Once both were in place, the browser's preflight got a clean `200 OK` with the right CORS headers, and the real authenticated `GET` request was allowed to proceed.

---

## 10. The `jjwt-impl` Runtime Error — What That Was

Separately from all the above, we hit a **library packaging issue**, not a logic bug:

The JWT library used (`io.jsonwebtoken`, aka JJWT) is split into three jars:
- `jjwt-api` — just interfaces/method signatures
- `jjwt-impl` — the actual working code behind those interfaces
- `jjwt-jackson` — JSON parsing support it needs internally

Code can **compile** successfully with only `jjwt-api` present, because the compiler only checks that method signatures exist — it doesn't run anything. But at **runtime**, when `Keys.hmacShaKeyFor()` actually executes, it needs the real implementation classes from `jjwt-impl`. If that jar isn't on the classpath, you get exactly the error seen: `ExceptionInInitializerError` — the class exists in name only, and Java can't find the actual code behind it.

**Fix:** make sure all three dependencies are declared in `build.gradle`, at matching versions, and that Gradle re-syncs (`./gradlew clean build --refresh-dependencies`) after adding them.

---

## Summary: The Full Journey, End to End

1. User logs in via Google OAuth → Auth module issues a signed JWT
2. Auth module redirects the browser to the frontend, token attached in the URL
3. Frontend saves the token, then must pass it across to whichever origin serves the next page (a workaround for the separate-apps setup)
4. Every subsequent API call attaches the token in the `Authorization` header
5. Each backend microservice independently verifies the token's signature using the shared HS256 secret, via its own `JwtAuthFilter` + `SecurityConfig`
6. CORS rules and explicit `OPTIONS` permission had to be configured so the browser's automatic preflight checks succeeded before the real authenticated request could go through
7. A missing runtime dependency (`jjwt-impl`) caused a separate, unrelated crash that looked confusing but was purely a build configuration gap