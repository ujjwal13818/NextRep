# NextSet 🏋️‍♂️

**AI-powered workout recommendation platform that tells you exactly what to train, how much weight to lift, and why — based on your real training history.**

No more guessing what to train today or what weight to load. NextSet looks at your last few sessions, your reported effort (RPE), and today's intensity level, then generates a personalized workout plan with set/rep/weight prescriptions — backed by a hybrid of deterministic sports-science logic and LLM-driven reasoning.

---

## 🎯 The Problem

Most workout logging apps are dumb spreadsheets — you tell *them* what you did. NextSet flips that: it tells *you* what to do next, using your own history, so progressive overload actually happens instead of being left to memory and guesswork.

---

## ✨ Features

### 1. Smart Muscle-Group Rotation
On "Start Workout," NextSet analyzes your last N sessions and recommends today's focus (e.g., "You hit chest/triceps yesterday and haven't trained legs in 5 days — here's a leg day plan").
- **How it's implemented:** A deterministic rotation algorithm running in the AI Recommendation Service, informed by aggregated session data from the Analytics Service. The LLM adds the human-readable rationale on top — it never invents the recommendation from scratch.

### 2. Intensity-Aware Planning
Before starting, you pick how you're feeling: **Low / Medium / High**. This directly adjusts the weight/set targets the system generates for the whole session.
- **How it's implemented:** A simple multiplier/adjustment layer applied to the progressive-overload output — low intensity trims volume or load ~10%, high intensity allows the full suggested increment.

### 3. AI-Suggested Sets, Reps & Weight
Pick an exercise, and NextSet suggests exactly how many sets, reps, and how much weight to load — based on your last performance for that specific exercise and your reported effort (RPE) last time.
- **How it's implemented:** Progressive-overload math (last weight × RPE-based adjustment) computes the candidate numbers. These are sent to the LLM, which can lightly refine and explain them — server-side validation clamps any AI adjustment to a safe ±10% range before it ever reaches the user.

### 4. Tap-First Logging (Zero Typing)
Every input during a workout — intensity, RPE, reps, weight — is a button, stepper, or toggle. No text fields, because nobody wants to type with sweaty hands mid-set.
- **How it's implemented:** React components using pre-filled steppers (defaulting to the AI's suggestion) and a 5-button RPE selector (Easy → Max) instead of raw numeric entry.

### 5. Bodyweight & Weighted Exercise Support
Toggle between bodyweight and external-load exercises per set — bodyweight logs skip the weight input entirely.

### 6. Progress Analytics
Behind the scenes, every logged set feeds a rolling analytics engine that tracks estimated 1-rep max (Epley formula), training volume, and frequency per muscle group — the same data that powers the recommendation engine.
- **How it's implemented:** Kafka event stream (`set.logged`) consumed asynchronously by the Analytics Service, decoupled from the main write path so logging a set stays fast regardless of how much analysis runs after it.

### 7. 15-Day PDF Workout Report
Download a PDF summary of your last 15 days: sessions trained, volume trends, and per-exercise progression.
- **How it's implemented:** Server-side PDF generation using JasperReports, pulling from the same aggregation logic used for AI recommendations.

### 8. Fast, Cached Recommendations
Re-opening a session doesn't re-trigger a fresh (slow, costly) LLM call — recommendations are cached per session.
- **How it's implemented:** Redis caches AI Recommendation Service output per user/session, plus the rarely-changing exercise catalog.

---

## 🏗️ Architecture

NextSet is built as a set of independent microservices behind a single API gateway:

```
                         ┌────────────────────┐
                         │   React Frontend    │
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
                         │    API Gateway       │
                         └──────────┬──────────┘
              ┌───────────┬─────────┼─────────┬───────────┐
              ▼           ▼         ▼         ▼           ▼
        ┌──────────┐ ┌─────────┐ ┌──────────────┐ ┌──────────────┐
        │   Auth   │ │ Workout │ │ AI Recommend │ │  Analytics    │
        │ Service  │ │ Service │ │   Service    │ │   Service     │
        └──────────┘ └────┬────┘ └──────┬───────┘ └───────┬──────┘
                           │             │                 │
                           │      ┌──────▼──────┐          │
                           │      │  LLM API    │          │
                           │      │(Claude/     │          │
                           │      │ Gemini)     │          │
                           │      └─────────────┘          │
                           │                                │
                     ┌─────▼────────────────────────────────▼─────┐
                     │           Apache Kafka (set.logged,          │
                     │            workout.completed events)         │
                     └───────────────────────────────────────────────┘
                                    │
                         ┌──────────▼──────────┐
                         │  PostgreSQL + Redis  │
                         └──────────────────────┘
```

**Key architectural principle:** *The LLM is never the sole source of a number.* Deterministic logic (rotation rules + progressive-overload formulas) computes the actual sets/reps/weight. The LLM only reorders, explains, or lightly adjusts within a clamped, validated range — so a hallucinated response can never produce an unsafe weight suggestion.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React |
| Backend | Java, Spring Boot (microservices) |
| Database | PostgreSQL |
| Caching | Redis |
| Event Streaming | Apache Kafka |
| AI / LLM | External LLM API (Claude / Gemini) |
| PDF Reports | JasperReports |
| Testing | JUnit, Mockito |
| Containerization | Docker, Docker Compose |
| Auth | Spring Security + JWT |

---

## 🗄️ Database Schema (PostgreSQL)

```sql
users (id, email, password_hash, bodyweight_kg, created_at)

exercise_catalog (id, name, muscle_group, equipment_type, is_bodyweight)

workout_sessions (id, user_id, date, muscle_group, intensity, status, created_at)

exercise_logs (id, workout_session_id, exercise_id, order_index)

set_logs (id, exercise_log_id, set_number, reps, weight_kg, is_bodyweight, rpe, logged_at)
```

- `intensity`: user-selected before starting a session — `low | medium | high`
- `rpe`: rate of perceived exertion (6–10 scale) logged per set, the key signal driving progressive-overload suggestions

---

## 🔌 Services Overview

| Service | Responsibility |
|---|---|
| **Auth Service** | Signup, login, JWT issuance |
| **Workout Service** | System of record — sessions, exercise logs, set logs, exercise catalog |
| **AI Recommendation Service** | Deterministic rotation + progressive-overload logic, then LLM-enriched output |
| **Analytics Service** | Consumes Kafka events, maintains 1RM/volume/frequency aggregates, generates PDF reports |
| **API Gateway** | Single entry point routing requests to the above services |

---

## 🤖 LLM Integration Contract

**Request (sent by AI Recommendation Service):**
```json
{
  "userId": "u123",
  "bodyWeightKg": 78,
  "todayIntensity": "medium",
  "recentSessions": [
    { "date": "2026-07-22", "muscleGroup": "chest_triceps", "totalVolume": 4200, "avgRpe": 7.5 }
  ],
  "perExerciseHistory": {
    "barbell_squat": { "lastWeightKg": 80, "lastReps": 8, "lastRpe": 8, "estimated1RM": 100 }
  },
  "candidateMuscleGroup": "legs",
  "candidateExercises": [
    { "exerciseId": "barbell_squat", "suggestedSets": 4, "suggestedReps": 8, "suggestedWeightKg": 82.5 }
  ]
}
```

**Response (strict JSON, validated server-side, clamped to ±10% of candidate values):**
```json
{
  "recommendedMuscleGroup": "legs",
  "rationale": "You trained legs 5 days ago and haven't hit posterior chain in over a week.",
  "exercises": [
    { "exerciseId": "barbell_squat", "sets": 4, "reps": 8, "weightKg": 82.5, "note": "Bumped 2.5kg since last RPE was 8." }
  ]
}
```

---

## 📡 Kafka Events

| Event | Published by | Consumed by | Purpose |
|---|---|---|---|
| `set.logged` | Workout Service | Analytics Service | Recompute 1RM estimate, volume, frequency stats |
| `workout.completed` | Workout Service | Analytics Service | Close out session aggregates |

---

## 🚀 Getting Started (Local Dev)

```bash
# Clone the repo
git clone https://github.com/amanPriyank/NextSet.git
cd NextSet

# Spin up Postgres, Redis, Kafka, and all services
docker-compose up --build

# Frontend
cd frontend
npm install
npm start
```

Environment variables required (see `.env.example` in each service):
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_BROKER_URL`
- `LLM_API_KEY`, `LLM_PROVIDER`
- `JWT_SECRET`

---

## 🧪 Testing

Unit and integration tests use **JUnit** and **Mockito**, covering:
- Progressive-overload calculation logic
- Rotation algorithm edge cases (new users, irregular training frequency)
- LLM response validation/clamping logic
- Kafka event publishing and consumption
- Repository and controller layers per service

```bash
mvn test
```

---

## 📌 Roadmap / Future Enhancements
- Streak tracking and notifications (additional Kafka consumer on `set.logged`)
- Injury/limitation-aware recommendations via free-form user input
- Mobile app (React Native)
- Social/peer features (optional, if expanding scope)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
