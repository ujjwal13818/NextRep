import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./Exercises.css";

const API_BASE = "http://localhost:8080/api/workouts";

const BODY_PART_IDS = {
  chest: "4616b8ed-d63d-4ac3-bb83-847a125f7f1f",
  back: "9ec98206-bdbf-447a-84fc-861045f442f9",
  shoulder: "549671d1-8f3d-44d9-b303-8dd7e8affa5e",
  neck: "161fa6b5-3c10-4fe7-9021-075713e106b3",
  cardio: "93f3c574-86ca-48ad-80c9-caa5cf657b2d",
  upperLeg: "bf3876ae-0acd-4381-aeb8-7b5180843878",
  lowerLeg: "3cad5129-2dfe-46fd-bac1-cf9a08d340fb",
  upperArm: "904f10fc-9489-4a3e-a07e-cb41fe4502fe",
  lowerArm: "01061dc7-7405-491c-92e7-db64aec401f1",
  waist: "55b98a42-7fe4-473f-8933-a722fd5a4ed1",
  biceps: "904f10fc-9489-4a3e-a07e-cb41fe4502fe",
  triceps: "904f10fc-9489-4a3e-a07e-cb41fe4502fe",
  forearm: "01061dc7-7405-491c-92e7-db64aec401f1",
  core: "55b98a42-7fe4-473f-8933-a722fd5a4ed1",
  leg: "bf3876ae-0acd-4381-aeb8-7b5180843878",
};

export default function Exercises() {
  const { bodyPart } = useParams();
  const navigate = useNavigate();
  const [exercises, setExercises] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [startingId, setStartingId] = useState(null);
  const [currentSessionId, setCurrentSessionId] = useState(null);

  useEffect(() => {
    const fetchExercises = async () => {
      setLoading(true);
      setError(null);

      const bodyPartId = BODY_PART_IDS[bodyPart];
      if (!bodyPartId) {
        setError(`No body part mapping found for "${bodyPart}"`);
        setLoading(false);
        return;
      }

      try {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(
          `${API_BASE}/body-part/${bodyPartId}/exercises`,
          { headers: { Authorization: `Bearer ${token}` } },
        );

        if (!response.ok)
          throw new Error(`Request failed with status ${response.status}`);

        const data = await response.json();
        setExercises(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchExercises();
  }, [bodyPart]);

  const handleExerciseClick = async (exercise) => {
    setStartingId(exercise.id);
    try {
      const token = localStorage.getItem("accessToken");
      const response = await fetch(
        `${API_BASE}/exercise-log/${exercise.id}/start`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (!response.ok)
        throw new Error(`Failed to start exercise log (${response.status})`);

      const data = await response.json();
      navigate(`/log/${data.id}`, {
        state: { exerciseName: data.exerciseName, sessionId: data.sessionId },
      });
    } catch (err) {
      setError(err.message);
      setStartingId(null);
    }
  };

  if (loading)
    return <div className="ExercisesStatus">Loading exercises...</div>;
  if (error)
    return <div className="ExercisesStatus ExercisesError">Error: {error}</div>;

  return (
    <div className="ExercisesPage">
      <button className="BackButton" onClick={() => navigate("/exercises")}>
        ← Back to Body Parts
      </button>
      <h2 className="ExercisesTitle">{bodyPart} Exercises</h2>

      {exercises.length === 0 ? (
        <p className="ExercisesEmpty">No exercises found for this body part.</p>
      ) : (
        <div className="ExercisesGrid">
          {exercises.map((exercise) => (
            <div
              key={exercise.id}
              className="ExerciseCard"
              onClick={() => handleExerciseClick(exercise)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ")
                  handleExerciseClick(exercise);
              }}
            >
              {exercise.demoUrl && (
                <img
                  src={exercise.demoUrl}
                  alt={exercise.name}
                  className="ExerciseImage"
                />
              )}
              <div className="ExerciseCardBody">
                <h3 className="ExerciseName">{exercise.name}</h3>
                <div className="ExerciseMetaRow">
                  <span className="ExerciseEquipment">
                    {exercise.equipment}
                  </span>
                  <span
                    className="ExerciseDifficulty"
                    data-difficulty={exercise.difficulty?.toLowerCase()}
                  >
                    {exercise.difficulty}
                  </span>
                </div>
                {exercise.instructions && (
                  <p className="ExerciseInstructions">
                    {exercise.instructions}
                  </p>
                )}
              </div>

              {startingId === exercise.id && (
                <div className="ExerciseCardLoading">Starting...</div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
