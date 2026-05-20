import { useState, useEffect } from "react";
import "./Recommendations.css";
import Navbar from "./Navbar";

const API_BASE = "http://localhost:8080";
const TMDB_IMG          = "https://image.tmdb.org/t/p/w500";
const TMDB_IMG_ORIGINAL = "https://image.tmdb.org/t/p/original";

const posterUrl = (path) =>
  path ? (path.startsWith("http") ? path : `${TMDB_IMG}${path}`) : null;

const fmt = (v) => (v && v > 0 ? Number(v).toFixed(1) : null);

// ── Movie Card ────────────────────────────────────────────────
function MovieCard({ movie, index, onClick }) {
  const [imgErr, setImgErr] = useState(false);

  return (
    <div
      className="rec-card"
      onClick={() => onClick(movie)}
      style={{ animationDelay: `${index * 0.06}s` }}
    >
      <div className="rec-card-poster">
        {posterUrl(movie.posterPath) && !imgErr ? (
          <img
            src={posterUrl(movie.posterPath)}
            alt={movie.title}
            className="rec-card-img"
            onError={() => setImgErr(true)}
          />
        ) : (
          <div className="rec-card-placeholder">
            <span>🎬</span>
            <span>{movie.title}</span>
          </div>
        )}
        {fmt(movie.rating) && (
          <div className="rec-card-badge">
            <svg width="9" height="9" viewBox="0 0 24 24" fill="#E8C547">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
            <span>{fmt(movie.rating)}</span>
          </div>
        )}
        <div className="rec-card-fade" />
      </div>
      <div className="rec-card-meta">
        <h3 className="rec-card-title">{movie.title}</h3>
        <div className="rec-card-bottom">
          {movie.releaseYear && (
            <span className="rec-card-year">{movie.releaseYear}</span>
          )}
          {movie.genres?.length > 0 && (
            <span className="rec-card-genre">{movie.genres[0]}</span>
          )}
        </div>
      </div>
    </div>
  );
}

const RATING_LABELS = { 0: "", 1: "Poor", 2: "Fair", 3: "Good", 4: "Great", 5: "Masterpiece" };

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
          <svg width="28" height="28" viewBox="0 0 24 24">
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

function Modal({ movie, onClose }) {
  const [imgErr, setImgErr]       = useState(false);
  const [rating, setRating]       = useState(0);
  const [saveStatus, setSaveStatus] = useState("idle"); // idle | saving | saved | error
  const [saveError, setSaveError] = useState("");

  const user = (() => {
    try { return JSON.parse(localStorage.getItem("user")); } catch { return null; }
  })();
  const userId = user?.id ?? user?.userId ?? null;

  const handleSave = async () => {
    if (!rating) return;
    setSaveStatus("saving");
    setSaveError("");
    try {
      const res = await fetch(`${API_BASE}/interactions/add`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId, movieId: movie.id, rating }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || `HTTP ${res.status}`);
      }
      setSaveStatus("saved");
      setTimeout(() => {
        onClose();
        window.location.reload();        // odświeża listę rekomendacji
      }, 700);
    } catch (err) {
      setSaveError(err.message);
      setSaveStatus("error");
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>×</button>
        <div className="modal-body">

          <div className="modal-poster-wrap">
            {posterUrl(movie.posterPath) && !imgErr ? (
              <img
                src={posterUrl(movie.posterPath)}
                alt={movie.title}
                className="modal-poster"
                onError={() => setImgErr(true)}
              />
            ) : (
              <div className="modal-poster-placeholder"><span>🎬</span></div>
            )}
          </div>

          <div className="modal-info">
            <h2 className="modal-title">{movie.title}</h2>

            <div className="modal-facts">
              {movie.releaseYear && (
                <span className="modal-fact">📅 {movie.releaseYear}</span>
              )}
              {fmt(movie.rating) && (
                <span className="modal-fact modal-fact--gold">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="#E8C547">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                  {fmt(movie.rating)}
                </span>
              )}
            </div>

            {movie.genres?.length > 0 && (
              <div className="modal-genres">
                {movie.genres.map((g) => (
                  <span key={g} className="modal-genre-tag">{g}</span>
                ))}
              </div>
            )}

            {movie.overview && (
              <p className="modal-overview">{movie.overview}</p>
            )}

            {movie.cast?.length > 0 && (
              <div className="modal-cast">
                <p className="modal-cast-label">Cast</p>
                <p className="modal-cast-list">{movie.cast.slice(0, 5).join(", ")}</p>
              </div>
            )}

            {/* ── RATING SECTION ── */}
            <div className="mrc-rating-section">
              <p className="mrc-rating-label">
                Your rating
                {rating > 0 && (
                  <span className="mrc-rating-hint"> — {RATING_LABELS[rating]}</span>
                )}
              </p>
              <StarRating value={rating} onChange={setRating} />
            </div>

            {saveStatus === "error" && (
              <div className="mrc-save-error">
                {saveError || "Failed to save rating"}
              </div>
            )}

            <div className="mrc-actions">
              <button
                className="btn-primary"
                onClick={handleSave}
                disabled={rating === 0 || saveStatus === "saving" || saveStatus === "saved"}
              >
                {saveStatus === "saving" ? "Saving…"
                  : saveStatus === "saved" ? "✓ Saved!"
                  : "Save rating"}
              </button>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

function WatchedList({ userId }) {
  const [watched, setWatched] = useState([]);
  const [status, setStatus]   = useState("loading");

  useEffect(() => {
    const fetchWatched = async () => {
      try {
        const res = await fetch(`${API_BASE}/interactions/${userId}/ratings`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        setWatched(Array.isArray(data) ? data : []);
        setStatus("done");
      } catch {
        setStatus("error");
      }
    };
    fetchWatched();
  }, [userId]);

  if (status === "loading") return (
    <div className="rec-loading" style={{ marginTop: "3rem" }}>
      <div className="rec-spinner" />
      <p>Loading watched movies…</p>
    </div>
  );

  if (status === "error" || watched.length === 0) return null;

  return (
    <div className="watched-section">
      <div className="rec-results-header">
        <span className="rec-results-count">
          <em>{watched.length}</em> movies you've rated
        </span>
      </div>
      <div className="watched-strip">
        {watched.map((m, i) => (
          <div key={m.id ?? i} className="watched-card">
            <div className="watched-poster">
              {posterUrl(m.posterPath) ? (
                <img
                  src={posterUrl(m.posterPath)}
                  alt={m.title}
                  className="watched-img"
                  onError={(e) => e.currentTarget.style.display = "none"}
                />
              ) : (
                <div className="watched-placeholder"><span>🎬</span></div>
              )}
              {m.userRating && (
                <div className="watched-badge">
                  <svg width="8" height="8" viewBox="0 0 24 24" fill="#E8C547">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                  {m.userRating}
                </div>
              )}
            </div>
            <p className="watched-title">{m.title}</p>
            <p className="watched-year">{m.releaseYear?.slice(0, 4)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────
export default function Recommendations() {
  const [movies, setMovies]   = useState([]);
  const [status, setStatus]   = useState("idle"); // idle | loading | error | done
  const [errorMsg, setErrorMsg] = useState("");
  const [selected, setSelected] = useState(null);

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

  const fetchRecommendations = async () => {
    setStatus("loading");
    setMovies([]);
    setErrorMsg("");

    try {
      const res = await fetch(`${API_BASE}/api/luceneRecommend/${userId}`);
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || `HTTP ${res.status}`);
      }
      const data = await res.json();
      setMovies(Array.isArray(data) ? data : []);
      setStatus("done");
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  };

  useEffect(() => {
    if (isLoggedIn) fetchRecommendations();
  }, []);

  // ── NOT LOGGED IN ──────────────────────────────────────────
  if (!isLoggedIn) {
    return (
      <div className="rec-app">
        <Navbar />
        <div className="rec-main">
          <div className="auth-wall">
            <div className="auth-wall-icon">
              <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="rgba(232,197,71,.5)" strokeWidth="1.5">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
            <h2 className="auth-wall-title">Sign in to see your recommendations</h2>
            <p className="auth-wall-sub">
              We build a personal list of films based on your ratings. Sign in to discover movies curated just for you.
            </p>
            <div className="auth-wall-steps">
              {[
                "Create an account or sign in",
                "Rate movies you've already seen",
                "Get personalized recommendations",
              ].map((s, i) => (
                <div key={i} className="auth-wall-step">
                  <span className="auth-wall-step-num">{i + 1}</span>
                  <span className="auth-wall-step-txt">{s}</span>
                </div>
              ))}
            </div>
            <div className="auth-wall-actions">
              <a href="/login" className="btn-primary">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
                Sign in
              </a>
              <a href="/register" className="btn-ghost">Create account</a>
            </div>
          </div>
          {/* WATCHED */}
        {isLoggedIn && <WatchedList userId={userId} />}
        </div>
      </div>
    );
  }

  // ── LOGGED IN ──────────────────────────────────────────────
  return (
    <div className="rec-app">
      <Navbar />

      <div className="rec-main">

        {/* HERO */}
        <div className="rec-hero">
          <p className="rec-eyebrow">Personalized for you</p>
          <h1 className="rec-title">
            Your <em>recommendations</em>
          </h1>
          <p className="rec-sub">
            Picked based on your ratings — the more you rate, the better they get
          </p>
        </div>

        {/* LOADING */}
        {status === "loading" && (
          <div className="rec-loading">
            <div className="rec-spinner" />
            <p>Fetching your recommendations…</p>
          </div>
        )}

        {/* ERROR */}
        {status === "error" && (
          <div className="rec-error-box">
            <p className="rec-error-title">⚠ Failed to load recommendations</p>
            <p className="rec-error-msg">{errorMsg}</p>
            <p className="rec-error-hint">
              Make sure the backend is running at <code>{API_BASE}</code>
            </p>
            <button className="btn-primary" onClick={fetchRecommendations}>
              Try again
            </button>
          </div>
        )}

        {/* EMPTY */}
        {status === "done" && movies.length === 0 && (
          <div className="rec-empty">
            <div className="rec-empty-icon">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="rgba(232,197,71,.3)" strokeWidth="1.5">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
            <h3 className="rec-empty-title">No recommendations yet</h3>
            <p className="rec-empty-sub">
              Rate a few movies first and we'll find something you'll love.
            </p>
            <a href="/rateMovie" className="btn-primary">
              Go rate movies
            </a>
          </div>
        )}

        {/* RESULTS */}
        {status === "done" && movies.length > 0 && (
          <>
            <div className="rec-results-header">
              <span className="rec-results-count">
                <em>{movies.length}</em> movies recommended for{" "}
                <span className="rec-results-name">{user.name}</span>
              </span>
              <button className="btn-ghost btn-ghost--sm" onClick={fetchRecommendations}>
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="23 4 23 10 17 10" />
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                </svg>
                Refresh
              </button>
            </div>

            <div className="rec-grid">
              {movies.map((m, i) => (
                <MovieCard
                  key={m.id ?? i}
                  movie={m}
                  index={i}
                  onClick={setSelected}
                />
              ))}
            </div>
          </>
        )}

      </div>

          {/* WATCHED */}
        {isLoggedIn && <WatchedList userId={userId} />}
        
      {selected && (
        <Modal movie={selected} 
        onClose={() => setSelected(null)} 
        onRated={fetchRecommendations}
        />
      )}

    </div>
  );
}
