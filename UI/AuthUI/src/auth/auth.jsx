// Auth.jsx
import "./auth.css";
import React from "react";

export const Auth = () => {
  const handleGoogleLogin = () => {
    window.location.href = "http://localhost:8081/oauth2/authorization/google";
  };

  return (
    <div className="AuthContainer">
      <div className="AuthCard">
        <div className="LogoBlock">
          <span className="LogoIcon">⚡</span>
          <h1 className="LogoText">
            NEXT<span className="LogoAccent">SET</span>
          </h1>
        </div>

        <p className="Tagline">TRAIN HARDER. TRACK SMARTER.</p>

        <button className="GoogleButton" onClick={handleGoogleLogin}>
          <svg
            className="GoogleIcon"
            viewBox="0 0 48 48"
            width="20"
            height="20"
          >
            <path
              fill="#FFC107"
              d="M43.6 20.5H42V20H24v8h11.3C33.7 32.9 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.1 8 3l6-6C34.6 5.1 29.6 3 24 3 12.4 3 3 12.4 3 24s9.4 21 21 21 21-9.4 21-21c0-1.2-.1-2.4-.4-3.5z"
            />
            <path
              fill="#FF3D00"
              d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.1 8 3l6-6C34.6 5.1 29.6 3 24 3c-7.6 0-14.1 4.3-17.7 10.7z"
            />
            <path
              fill="#4CAF50"
              d="M24 45c5.5 0 10.5-1.9 14.3-5.1l-6.6-5.6C29.7 35.9 27 37 24 37c-5.3 0-9.7-3.1-11.3-7.6l-6.6 5.1C9.8 40.6 16.4 45 24 45z"
            />
            <path
              fill="#1976D2"
              d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.2 4.3-4.1 5.7l6.6 5.6C41.4 36.4 44 30.7 44 24c0-1.2-.1-2.4-.4-3.5z"
            />
          </svg>
          Sign in with Google
        </button>

        <p className="FootNote">No excuses. No spreadsheets. Just reps.</p>
      </div>
    </div>
  );
};
