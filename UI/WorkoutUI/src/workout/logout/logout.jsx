const API_BASE = "http://localhost:8080/api/workouts";
const AUTH_APP_URL = "http://localhost:5173/auth";

export async function handleLogout() {
  const token = localStorage.getItem("accessToken");

  try {
    if (token) {
      await fetch(`${API_BASE}/session/complete-current`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  } catch (err) {
    console.error("Failed to auto-complete session on logout:", err);
  } finally {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.location.href = AUTH_APP_URL;
  }
}