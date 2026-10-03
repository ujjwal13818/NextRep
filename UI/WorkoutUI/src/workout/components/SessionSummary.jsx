import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./SessionSummary.css";

const API_BASE = "http://localhost:8080/api/workouts";

export default function SessionSummary() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [finishing, setFinishing] = useState(false);

  const fetchSummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem("accessToken");
      const response = await fetch(`${API_BASE}/session/${sessionId}/summary`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error(`Request failed (${response.status})`);
      const data = await response.json();
      setSummary(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();
  }, [sessionId]);

  const handleFinishWorkout = async () => {
    setFinishing(true);
    setError(null);
    try {
      const token = localStorage.getItem("accessToken");
      const response = await fetch(`${API_BASE}/session/${sessionId}/complete`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error(`Failed to complete session (${response.status})`);
      await fetchSummary(); // refresh to show COMPLETED status
    } catch (err) {
      setError(err.message);
    } finally {
      setFinishing(false);
    }
  };

  if (loading) return <div className="SummaryStatus">Loading session...</div>;
  if (error) return <div className="SummaryStatus SummaryError">Error: {error}</div>;
  if (!summary) return null;

  const isCompleted = summary.status === "COMPLETED";

  // Only show exercises that actually have at least one logged set
  const exercisesWithSets = summary.exercises.filter((ex) => ex.sets.length > 0);

  return (
    <div className="SessionSummaryPage">
      <button className="BackButton" onClick={() => navigate("/exercises")}>
        ← Back to Body Parts
      </button>

      <div className="SummaryHeader">
        <h2 className="SummaryTitle">Workout Summary</h2>
        <span className={`SummaryStatusBadge ${isCompleted ? "Completed" : "InProgress"}`}>
          {summary.status}
        </span>
      </div>

      {exercisesWithSets.length === 0 ? (
        <p className="SummaryEmpty">No exercises logged yet.</p>
      ) : (
        <div className="SummaryExerciseList">
          {exercisesWithSets.map((ex) => (
            <div key={ex.workoutExerciseLogId} className="SummaryExerciseCard">
              <div className="SummaryExerciseHeader">
                <h3 className="SummaryExerciseName">{ex.exerciseName}</h3>
                <button
                  className="AddSetLink"
                  onClick={() =>
                    navigate(`/log/${ex.workoutExerciseLogId}`, {
                      state: { exerciseName: ex.exerciseName, sessionId },
                    })
                  }
                >
                  + Add Set
                </button>
              </div>

              <div className="SummarySetsList">
                {ex.sets.map((set) => (
                  <div key={set.id} className="SummarySetRow">
                    <span>Set {set.setNumber}</span>
                    <span>{set.reps} reps</span>
                    {set.weight != null && <span>{set.weight} kg</span>}
                    <span
                      className="SummaryIntensity"
                      data-intensity={set.intensity}
                    >
                      {set.intensity}/5
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {!isCompleted && (
        <button
          className="FinishWorkoutButton"
          onClick={handleFinishWorkout}
          disabled={finishing}
        >
          {finishing ? "Finishing..." : "Finish Workout"}
        </button>
      )}
    </div>
  );
}