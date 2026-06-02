import { useState, useEffect } from "react";
import Navbar from "./Navbar";
import "./RateMovie.css";

const API_BASE = "http://localhost:8080";

function StarRating({ value, onChange }) {
  const [hovered, setHovered] = useState(0);
  const active = hovered || value;

  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          className={`star-btn ${active >= n ? "star-btn--on" : ""}`}
          onMouseEnter={() => setHovered(n)}
          onMouseLeave={() => setHovered(0)}
          onClick={() => onChange(n)}
          aria-label={`Rate ${n} out of 5`}
        >
          <svg width="32" height="32" viewBox="0 0 24 24">
            <polygon
              points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
              fill={active >= n ? "#E8C547" : "none"}
              stroke={active >= n ? "#E8C547" : "rgba(255,255,255,0.15)"}
              strokeWidth="1.5"
            />
          </svg>
        </button>
      ))}
    </div>
  );
}

const RATING_LABELS = {
  0: "",
  1: "Poor",
  2: "Fair",
  3: "Good",
  4: "Great",
  5: "Masterpiece",
};

export default function RateMovie() {
  const [movie, setMovie]       = useState(null);
  const [loadStatus, setLoadStatus] = useState("idle"); // idle | loading | error | empty
  const [rating, setRating]     = useState(0);
  const [saveStatus, setSaveStatus] = useState("idle"); // idle | saving | saved | error
  const [saveError, setSaveError]   = useState("");
  const [skipped, setSkipped]   = useState(0); // count of "not seen" skips

  // Read user from localStorage — adjust key to match your auth setup
  const user = (() => {
    try {
      const raw = localStorage.getItem("user");
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  })();

  const userId = user?.id ?? user?.userId ?? null;
  const isLoggedIn = userId !== null;

  const fetchMovie = async () => {
    if (!isLoggedIn) return;
    setLoadStatus("loading");
    setMovie(null);
    setRating(0);
    setSaveStatus("idle");
    setSaveError("");

    try {
      const res = await fetch(`${API_BASE}/api/to-rate/${userId}`, {
        credentials: 'include',
      });
      if (res.status === 204 || res.status === 404) {
        setLoadStatus("empty");
        return;
      }
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setMovie(data);
      setLoadStatus("done");
    } catch (err) {
      setLoadStatus("error");
    }
  };

  useEffect(() => {
    if (isLoggedIn) fetchMovie();
  }, []);

  const handleNotSeen = () => {
    setSkipped((s) => s + 1);
    fetchMovie();
  };

  const handleSave = async () => {
    if (!rating || !movie) return;
    setSaveStatus("saving");
    setSaveError("");

    try {
      const res = await fetch(`${API_BASE}/interactions/add`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: 'include',
        body: JSON.stringify({
          userId:  userId,
          movieId: movie.id,
          rating:  rating,
        }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || `HTTP ${res.status}`);
      }
      setSaveStatus("saved");
      setTimeout(() => fetchMovie(), 900);
    } catch (err) {
      setSaveError(err.message);
      setSaveStatus("error");
    }
  };

  // ── NOT LOGGED IN ─────────────────────────────────────────
  if (!isLoggedIn) {
    return (
      <div className="rate-app">
        <Navbar />
        <div className="rate-main">
          <div className="auth-wall">
            <div className="auth-wall-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="rgba(232,197,71,.5)" strokeWidth="1.5">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <h2 className="auth-wall-title">Sign in to rate movies</h2>
            <p className="auth-wall-sub">
              You need to be logged in to rate movies and build your personal recommendations.
            </p>
            <div className="auth-wall-actions">
              <a href="/login" className="btn-primary">Sign in</a>
              <a href="/register" className="btn-ghost">Create account</a>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ── MAIN RENDER ───────────────────────────────────────────
  return (
    <div className="rate-app">
      <Navbar />

      <div className="rate-main">

        <div className="rate-hero">
          <p className="rate-eyebrow">Rate movies</p>
          <h1 className="rate-title">How did you find<br /><em>this one?</em></h1>
          <p className="rate-sub">Rate movies you've seen to get better recommendations</p>
          {skipped > 0 && (
            <span className="skipped-badge">{skipped} skipped</span>
          )}
        </div>

        <div className="rate-stage">

          {/* LOADING */}
          {loadStatus === "loading" && (
            <div className="rate-loading">
              <div className="rate-spinner" />
              <p>Loading next movie…</p>
            </div>
          )}

          {/* ERROR */}
          {loadStatus === "error" && (
            <div className="rate-msg-box rate-msg-box--error">
              <p className="rate-msg-title">⚠ Failed to load movie</p>
              <p className="rate-msg-sub">Make sure the backend is running at <code>{API_BASE}</code></p>
              <button className="btn-primary" onClick={fetchMovie}>Try again</button>
            </div>
          )}

          {/* EMPTY */}
          {loadStatus === "empty" && (
            <div className="rate-msg-box">
              <div className="rate-msg-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#E8C547" strokeWidth="1.5">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
              </div>
              <h3 className="rate-msg-title">All caught up!</h3>
              <p className="rate-msg-sub">You've rated all available movies. Check back later.</p>
            </div>
          )}

          {/* MOVIE CARD */}
          {loadStatus === "done" && movie && (
            <div className="movie-rate-card">

              {/* POSTER */}
              <div className="mrc-poster-wrap">
                {movie.posterPath ? (
                  <img src={movie.posterPath} alt={movie.title} className="mrc-poster" />
                ) : (
                  <div className="mrc-poster-placeholder"><span>🎬</span></div>
                )}
                <div className="mrc-poster-glow" />
              </div>

              {/* INFO */}
              <div className="mrc-info">

                <div className="mrc-meta">
                  {movie.releaseYear && (
                    <span className="mrc-pill">{movie.releaseYear}</span>
                  )}
                  {movie.rating > 0 && (
                    <span className="mrc-pill mrc-pill--gold">
                      <svg width="9" height="9" viewBox="0 0 24 24" fill="#E8C547">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                      </svg>
                      {Number(movie.rating).toFixed(1)}
                    </span>
                  )}
                  {movie.genres?.length > 0 && movie.genres.map((g) => (
                    <span key={g} className="mrc-pill">{g}</span>
                  ))}
                </div>

                <h2 className="mrc-title">{movie.title}</h2>

                {movie.overview && (
                  <p className="mrc-overview">{movie.overview}</p>
                )}

                {/* RATING */}
                <div className="mrc-rating-section">
                  <p className="mrc-rating-label">
                    Your rating
                    {rating > 0 && (
                      <span className="mrc-rating-hint"> — {RATING_LABELS[rating]}</span>
                    )}
                  </p>
                  <StarRating value={rating} onChange={setRating} />
                </div>

                {/* SAVE ERROR */}
                {saveStatus === "error" && (
                  <div className="mrc-save-error">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#e05555" strokeWidth="2">
                      <circle cx="12" cy="12" r="10"/>
                      <line x1="12" y1="8" x2="12" y2="12"/>
                      <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    {saveError || "Failed to save rating"}
                  </div>
                )}

                {/* ACTIONS */}
                <div className="mrc-actions">
                  <button
                    className="btn-primary"
                    onClick={handleSave}
                    disabled={rating === 0 || saveStatus === "saving" || saveStatus === "saved"}
                  >
                    {saveStatus === "saving" ? (
                      <><div className="spinner" /> Saving…</>
                    ) : saveStatus === "saved" ? (
                      <>
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                          <polyline points="20 6 9 17 4 12"/>
                        </svg>
                        Saved!
                      </>
                    ) : (
                      <>
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                          <polyline points="20 6 9 17 4 12"/>
                        </svg>
                        Save rating
                      </>
                    )}
                  </button>

                  <button className="btn-ghost" onClick={handleNotSeen}>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                    Haven't seen it
                  </button>
                </div>

              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
