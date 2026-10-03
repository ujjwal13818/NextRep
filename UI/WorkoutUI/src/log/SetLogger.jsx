import React, { useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import "./SetLogger.css";

const API_BASE = "http://localhost:8080/api/workouts";

export default function SetLogger() {
  const { workoutExerciseLogId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const exerciseName = location.state?.exerciseName ?? "Exercise";
  const sessionId = location.state?.sessionId; // NEW

  const [loggedSets, setLoggedSets] = useState([]);
  const [reps, setReps] = useState("");
  const [weight, setWeight] = useState("");
  const [intensity, setIntensity] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const nextSetNumber = loggedSets.length + 1;

  const canSubmit = reps !== "" && intensity !== null && !saving;

  const handleLogSet = async () => {
    if (!canSubmit) return;
    setSaving(true);
    setError(null);

    try {
      const token = localStorage.getItem("accessToken");
      const response = await fetch(
        `${API_BASE}/exercise-log/${workoutExerciseLogId}/sets`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            setNumber: nextSetNumber,
            reps: Number(reps),
            weight: weight === "" ? null : Number(weight),
            intensity,
          }),
        },
      );

      if (!response.ok)
        throw new Error(`Failed to log set (${response.status})`);

      const data = await response.json();
      setLoggedSets((prev) => [...prev, data]);

      setReps("");
      setIntensity(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="SetLoggerPage">
      <button className="BackButton" onClick={() => navigate(-1)}>
        ← Back
      </button>

      <h2 className="SetLoggerTitle">{exerciseName}</h2>
      {sessionId && (
        <button
          className="ViewSummaryLink"
          onClick={() => navigate(`/session/${sessionId}`)}
        >
          Workout Summary <span aria-hidden="true">→</span>
        </button>
      )}

      {loggedSets.length > 0 && (
        <div className="LoggedSetsList">
          {loggedSets.map((set) => (
            <div key={set.id} className="LoggedSetRow">
              <span className="LoggedSetNumber">Set {set.setNumber}</span>
              <span className="LoggedSetDetail">{set.reps} reps</span>
              {set.weight != null && (
                <span className="LoggedSetDetail">{set.weight} kg</span>
              )}
              <span
                className="LoggedSetIntensity"
                data-intensity={set.intensity}
              >
                {set.intensity}/5
              </span>
            </div>
          ))}
        </div>
      )}

      <div className="SetLoggerForm">
        <div className="SetLoggerFieldRow">
          <label className="SetLoggerLabel">Reps</label>
          <input
            type="number"
            inputMode="numeric"
            className="SetLoggerInput"
            value={reps}
            onChange={(e) => setReps(e.target.value)}
            placeholder="10"
          />
        </div>

        <div className="SetLoggerFieldRow">
          <label className="SetLoggerLabel">Weight (kg)</label>
          <input
            type="number"
            inputMode="decimal"
            className="SetLoggerInput"
            value={weight}
            onChange={(e) => setWeight(e.target.value)}
            placeholder="optional"
          />
        </div>

        <div className="SetLoggerFieldRow">
          <label className="SetLoggerLabel">Intensity</label>
          <div className="IntensityPicker">
            {[1, 2, 3, 4, 5].map((level) => (
              <button
                key={level}
                type="button"
                className={`IntensityButton ${intensity === level ? "IntensityButtonActive" : ""}`}
                data-level={level}
                onClick={() => setIntensity(level)}
              >
                {level}
              </button>
            ))}
          </div>
        </div>

        {error && <p className="SetLoggerError">{error}</p>}

        <button
          className="LogSetButton"
          disabled={!canSubmit}
          onClick={handleLogSet}
        >
          {saving ? "Saving..." : `Log Set ${nextSetNumber}`}
        </button>
      </div>
    </div>
  );
}