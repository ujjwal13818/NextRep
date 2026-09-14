import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import './index.css'
import { HomePage } from './workout/HomePage.jsx'
import Exercises from './workout/components/Exercises.jsx';

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/exercises" replace />} />
        <Route path="/exercises" element={<HomePage />} />
        <Route path="/exercises/body-part/:bodyPart" element={<Exercises />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
);
