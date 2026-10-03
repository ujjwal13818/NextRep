import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import './index.css'
import { HomePage } from './workout/HomePage.jsx'
import Exercises from './workout/components/Exercises.jsx';
import SetLogger from './log/SetLogger.jsx';
import SessionSummary from './workout/components/SessionSummary.jsx';
import Layout from './Layout/Layout.jsx';

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/exercises" replace />} />

        <Route element={<Layout />}>
          <Route path="/exercises" element={<HomePage />} />
          <Route path="/exercises/body-part/:bodyPart" element={<Exercises />} />
          <Route path="/log/:workoutExerciseLogId" element={<SetLogger />} />
          <Route path="/session/:sessionId" element={<SessionSummary />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
);