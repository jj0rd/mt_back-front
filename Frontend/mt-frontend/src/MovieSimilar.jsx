import { useState } from "react";
import "./MovieSimilar.css";
import Navbar from "./Navbar";

const TMDB_IMG = "https://image.tmdb.org/t/p/w500";
const posterUrl = (path) => (path ? `${TMDB_IMG}${path}` : null);
const year = (date) => (date ? date.slice(0, 4) : "—");
const fmt = (v) => (v ? Number(v).toFixed(1) : "N/A");
function StarBar({ value }) {
  return (
    <div className="star-bar">
      <div className="star-track">
        <div className="star-fill" style={{ width: `${Math.round((value / 10) * 100)}%` }} />
      </div>
      <span className="star-value">{fmt(value)}</span>
    </div>
  );
}

function ResultCard({ movie, index, onClick }) {
  const [imgErr, setImgErr] = useState(false);
  const img = posterUrl(movie.poster_path);

  return (
    <div className="rcard" onClick={() => onClick(movie)} style={{ animationDelay: `${index * 0.07}s` }}>
      <div className="rcard-poster">
        {img && !imgErr ? (
          <img src={img} alt={movie.title} className="rcard-img" onError={() => setImgErr(true)} />
        ) : (
          <div className="rcard-placeholder">
            <span>🎬</span>
            <span>{movie.title}</span>
          </div>
        )}
        <div className="rcard-badge">
          <svg width="9" height="9" viewBox="0 0 24 24" fill="#E8C547">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
          <span>{fmt(movie.vote_average)}</span>
        </div>
        <div className="rcard-fade" />
      </div>
      <div className="rcard-meta">
        <h3 className="rcard-title">{movie.title}</h3>
        <span className="rcard-year">{year(movie.release_date)}</span>
      </div>
    </div>
  );
}

function Modal({ movie, onClose }) {
  const [imgErr, setImgErr] = useState(false);
  const poster = posterUrl(movie.poster_path);
  const backdrop = movie.backdrop_path
    ? `https://image.tmdb.org/t/p/original${movie.backdrop_path}`
    : null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        {backdrop && (
          <div className="modal-backdrop">
            <img src={backdrop} alt="" />
            <div className="modal-backdrop-fade" />
          </div>
        )}
        <button className="modal-close" onClick={onClose}>×</button>
        <div className={`modal-body${backdrop ? " modal-body--with-backdrop" : ""}`}>
          <div className="modal-poster-wrap">
            {poster && !imgErr ? (
              <img src={poster} alt={movie.title} className="modal-poster" onError={() => setImgErr(true)} />
            ) : (
              <div className="modal-poster-placeholder"><span>🎬</span></div>
            )}
          </div>
          <div className="modal-info">
            <h2 className="modal-title">{movie.title}</h2>
            {movie.original_title && movie.original_title !== movie.title && (
              <p className="modal-original">{movie.original_title}</p>
            )}
            <div className="modal-facts">
              <span className="modal-fact">📅 {year(movie.release_date)}</span>
              <span className="modal-fact">🌐 {movie.original_language?.toUpperCase() || "—"}</span>
              {movie.vote_count > 0 && (
                <span className="modal-fact">👥 {movie.vote_count.toLocaleString()}</span>
              )}
            </div>
            <StarBar value={movie.vote_average} />
            <p className="modal-overview">{movie.overview || "No description available."}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function MovieSimilar() {
  const [movie1, setMovie1] = useState("");
  const [movie2, setMovie2] = useState("");
  const [results, setResults] = useState([]);
  const [inputMovies, setInputMovies] = useState([]);
  const [status, setStatus] = useState("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [selected, setSelected] = useState(null);

  const canSubmit = movie1.trim().length > 0 && movie2.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setStatus("loading");
    setResults([]);
    setErrorMsg("");

    try {
      const res = await fetch("http://localhost:8080/api/similar", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ movieTitles: [movie1.trim(), movie2.trim()] }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      setResults(data.movies || []);
      setInputMovies(data.inputMovies || [movie1.trim(), movie2.trim()]);
      setStatus("done");
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  };

  const handleReset = () => {
    setMovie1("");
    setMovie2("");
    setResults([]);
    setInputMovies([]);
    setStatus("idle");
    setErrorMsg("");
  };

  const handleKey = (e) => {
    if (e.key === "Enter" && canSubmit) handleSubmit();
  };

  return (
    <div className="app">

    <Navbar />

      {/* <nav className="nav">
        <div className="nav-inner">
          <div className="nav-brand">
            <div className="nav-logo">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="#0a0908">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
            <span className="nav-title">CineMatch</span>
          </div>
          <span className="nav-badge">AI · TMDB</span>
        </div>
      </nav> */}

      <div className="main">
        <div className="hero">
          <p className="hero-eyebrow">Similarity Engine</p>
          <h1 className="hero-title">
            Two films,<br /><em>endless discoveries</em>
          </h1>
          <p className="hero-sub">Enter two movies you love — we'll find what connects them</p>
        </div>

        <div className="input-section">
          <div className="movies-row">
            <div className="movie-input-wrap">
              <span className="movie-input-label">First movie</span>
              <input
                className={`movie-input${movie1.trim() ? " filled" : ""}`}
                type="text"
                value={movie1}
                onChange={e => setMovie1(e.target.value)}
                onKeyDown={handleKey}
                placeholder="e.g. Inception"
              />
            </div>

            <div className="plus-divider">+</div>

            <div className="movie-input-wrap">
              <span className="movie-input-label">Second movie</span>
              <input
                className={`movie-input${movie2.trim() ? " filled" : ""}`}
                type="text"
                value={movie2}
                onChange={e => setMovie2(e.target.value)}
                onKeyDown={handleKey}
                placeholder="e.g. Interstellar"
              />
            </div>
          </div>

          <div className="submit-row">
            <button
              className="btn-primary"
              onClick={handleSubmit}
              disabled={!canSubmit || status === "loading"}
            >
              {status === "loading" ? (
                <><div className="spinner" /> Finding…</>
              ) : (
                <>
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                  </svg>
                  Find similar movies
                </>
              )}
            </button>
            {(movie1 || movie2 || status !== "idle") && (
              <button className="btn-ghost" onClick={handleReset}>Clear</button>
            )}
          </div>
        </div>

        {/* STATES */}
        {status === "idle" && (
          <div className="idle-state">
            <div className="idle-films">
              <div className="idle-film-stub"><span>🎬</span></div>
              <span className="idle-plus">+</span>
              <div className="idle-film-stub"><span>🎬</span></div>
              <span className="idle-arrow">→</span>
              <div className="idle-reel"><span>✦</span></div>
              <div className="idle-reel"><span>✦</span></div>
              <div className="idle-reel"><span>✦</span></div>
            </div>
            <h3 className="idle-title">Your next favorites are waiting</h3>
            <p className="idle-sub">Type two movies above and press Enter or click the button</p>
          </div>
        )}

        {status === "error" && (
          <div className="error-box">
            <p className="error-title">⚠ Request error</p>
            <p className="error-msg">{errorMsg}</p>
            <p className="error-hint">Make sure the backend is running at <code>http://localhost:8080</code></p>
          </div>
        )}

        {status === "done" && results.length === 0 && (
          <div className="results-empty">
            <div className="empty-icon">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.15)" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
              </svg>
            </div>
            <h3 className="empty-title">No results found</h3>
            <p className="empty-sub">Try different movie titles or check your spelling</p>
          </div>
        )}

        {status === "done" && results.length > 0 && (
          <div className="results-section">
            <div className="results-header">
              <h2 className="results-title">Similar movies</h2>
              <div className="results-meta">
                <span className="input-tag">{inputMovies[0]}</span>
                <span className="input-tag-sep">+</span>
                <span className="input-tag">{inputMovies[1]}</span>
                <span className="results-count">→ <em>{results.length}</em> found</span>
              </div>
            </div>
            <div className="results-grid">
              {results.map((m, i) => (
                <ResultCard key={m.id ?? i} movie={m} index={i} onClick={setSelected} />
              ))}
            </div>
          </div>
        )}
      </div>

      {selected && <Modal movie={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}
