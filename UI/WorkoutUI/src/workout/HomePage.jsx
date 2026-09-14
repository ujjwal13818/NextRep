import React from 'react'
import './HomePage.css'
import { useEffect, useState } from "react";
import { DaysCards } from './components/DaysCards';

export const HomePage = () => {

    const [days, setDays] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
       const params = new URLSearchParams(window.location.search);
       const token = params.get("token");
       const refreshToken = params.get("refreshToken");

       if (token && refreshToken) {
         localStorage.setItem("accessToken", token);
         localStorage.setItem("refreshToken", refreshToken);
         window.history.replaceState({}, document.title, "/exercises"); // clean URL
       }
       
      const fetchDays = async () => {
        const token = localStorage.getItem("accessToken");

        if (!token) {
          setError("No auth token found. Please log in again.");
          setLoading(false);
          return;
        }

        try {
          const response = await fetch(
            "http://localhost:8080/api/workouts/days",
            {
              headers: {
                Authorization: `Bearer ${token}`,
              },
            },
          );

          if (!response.ok) {
            throw new Error(`Request failed: ${response.status}`);
          }

          const data = await response.json();
          setDays(data);
        } catch (err) {
          console.error("Failed to fetch workout days:", err);
          setError("Could not load workout days.");
        } finally {
          setLoading(false);
        }
      };

      fetchDays();
    }, []);

     if (loading) return <p>Loading...</p>;
     if (error) return <p className="CallbackTextError">{error}</p>;

   return (
     <div className="home-page">
       <h1>NextSet: Your Next Rep Starts Here</h1>
       <DaysCards days={days} />
     </div>
   );
}