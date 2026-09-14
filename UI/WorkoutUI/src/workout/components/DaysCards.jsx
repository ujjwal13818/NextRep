import React from "react";
import { useNavigate } from "react-router-dom";
import "./DaysCards.css";

const MUSCLE_GROUP_META = {
  chest: {
    image: "/Chest.jpg",
    line: "Bench day. Everyone's favorite — show up and prove it.",
    theme: "theme-crimson",
  },
  back: {
    image: "/back.jpg",
    line: "Nobody sees back day in the mirror. Do it anyway.",
    theme: "theme-navy",
  },
  leg: {
    image: "/leg.jpg",
    line: "The most skipped day. The one that separates you from most people.",
    theme: "theme-forest",
  },
  shoulder: {
    image: "/shoulder.png",
    line: "Build the width that makes everything else look bigger.",
    theme: "theme-amber",
  },
  biceps: {
    image: "/Biceps.jfif",
    line: "Fine, do the mirror muscle. Just don't skip the rest.",
    theme: "theme-plum",
  },
  triceps: {
    image: "/triceps.jfif",
    line: "Two-thirds of your arm is here, not in the curls.",
    theme: "theme-slate",
  },
  forearm: {
    image: "/Forearms.jfif",
    line: "Grip strength quietly limits every lift you do. Fix it here.",
    theme: "theme-teal",
  },
  core: {
    image: "/core.jfif",
    line: "Every heavy lift starts and ends with this staying tight.",
    theme: "theme-crimson",
  },
  pull: {
    image: "/Pull.jfif",
    line: "Pull day. Balance out all that pushing.",
    theme: "theme-navy",
  },
  push: {
    image: "/push.jfif",
    line: "Push day. Chest, shoulders, triceps — all in one shot.",
    theme: "theme-forest",
  },
};

const FALLBACK_THEMES = [
  "theme-crimson",
  "theme-forest",
  "theme-navy",
  "theme-amber",
  "theme-plum",
  "theme-slate",
  "theme-teal",
];

const normalize = (rawName = "") => rawName.toLowerCase().replace(/_day$/, "");

const toLabel = (rawName = "") =>
  rawName
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

export const DaysCards = ({ days }) => {
  const navigate = useNavigate();

  const handleCardClick = (bodyPartKey) => {
    navigate(`/exercises/body-part/${bodyPartKey}`);
  };

  return (
    <div className="DaysGrid">
      {days.map((dayName, index) => {
        const key = normalize(dayName);
        const meta = MUSCLE_GROUP_META[key];

        const label = toLabel(dayName);
        const imageUrl = meta?.image ?? null;
        const motivationLine =
          meta?.line ?? "Every session counts. Get in there.";
        const theme =
          meta?.theme ?? FALLBACK_THEMES[index % FALLBACK_THEMES.length];

        return (
          <div
            key={dayName}
            className={`DayCard ${theme}`}
            onClick={() => handleCardClick(key)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") handleCardClick(key);
            }}
          >
            <div className="DayImagePlaceholder">
              {imageUrl ? (
                <img src={imageUrl} alt={label} className="DayImage" />
              ) : (
                <span className="PlaceholderIcon">+</span>
              )}
            </div>

            <div className="DayCardOverlay">
              <h3 className="DayCardTitle">{label}</h3>
              <p className="DayCardMotivation">{motivationLine}</p>
            </div>
          </div>
        );
      })}
    </div>
  );
};
