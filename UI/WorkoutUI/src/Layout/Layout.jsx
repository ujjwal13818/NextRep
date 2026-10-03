import React, { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { handleLogout } from "../workout/logout/logout.jsx";
import "./Layout.css";

const API_BASE = "http://localhost:8080/api/workouts";

export default function Layout() {
  const navigate = useNavigate();
  const [currentSessionId, setCurrentSessionId] = useState(null);

  useEffect(() => {
    const checkCurrentSession = async () => {
      const token = localStorage.getItem("accessToken");
      if (!token) return;

      try {
        const response = await fetch(`${API_BASE}/session/current`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 200) {
          const data = await response.json();
          setCurrentSessionId(data.id);
        }
      } catch (err) {
        console.error("Failed to check current session:", err);
      }
    };

    checkCurrentSession();
  }, []);

  return (
    <div className="AppShell">
      <div className="GlobalTopBar">
        {currentSessionId && (
          <button
            className="GlobalSummaryButton"
            onClick={() => navigate(`/session/${currentSessionId}`)}
          >
            Workout Summary <span aria-hidden="true">→</span>
          </button>
        )}

        <button className="GlobalLogoutButton" onClick={handleLogout}>
          Logout
        </button>
      </div>

      <Outlet />
    </div>
  );
}