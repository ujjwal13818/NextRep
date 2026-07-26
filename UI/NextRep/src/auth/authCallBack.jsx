import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import "./AuthCallback.css";

function AuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState("processing"); // 'processing' | 'error'

  useEffect(() => {
    const token = searchParams.get("token");
    const refreshToken = searchParams.get("refreshToken");

    if (token && refreshToken) {
      localStorage.setItem("accessToken", token);
      localStorage.setItem("refreshToken", refreshToken);

      // Clean the URL so tokens don't linger in browser history
      window.history.replaceState({}, document.title, "/auth/callback");

      navigate("/dashboard", { replace: true });
    } else {
      setStatus("error");
      const timeout = setTimeout(() => {
        navigate("/auth", { replace: true });
      }, 2000);

      return () => clearTimeout(timeout);
    }
  }, [searchParams, navigate]);

  return (
    <div className="CallbackContainer">
      {status === "processing" ? (
        <>
          <div className="Spinner" />
          <p className="CallbackText">Logging you in...</p>
        </>
      ) : (
        <p className="CallbackTextError">
          Something went wrong. Redirecting you back...
        </p>
      )}
    </div>
  );
}

export default AuthCallback;
