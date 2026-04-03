import { useState, useRef, useCallback } from "react";
import "./MovieSearch.css";
import Navbar from "./Navbar";

const API_BASE          = "http://localhost:8080/api/tmdb/movies";
const TMDB_IMG          = "https://image.tmdb.org/t/p/w500";
const TMDB_IMG_ORIGINAL = "https://image.tmdb.org/t/p/original";

const posterUrl   = (path) => (path ? `${TMDB_IMG}${path}`          : null);
const backdropUrl = (path) => (path ? `${TMDB_IMG_ORIGINAL}${path}` : null);
const year        = (date) => (date ? date.slice(0, 4)               : "—");
const fmt         = (v)    => (v    ? v.toFixed(1)                   : "N/A");

// ── StarBar ──────────────────────────────────────────────────
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

// ── Modal ────────────────────────────────────────────────────
function Modal({ movie, onClose }) {
  const [imgErr, setImgErr] = useState(false);
  const backdrop = backdropUrl(movie.backdrop_path);
  const poster   = posterUrl(movie.poster_path);

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

          {/* poster */}
          <div className="modal-poster-wrap">
            {poster && !imgErr ? (
              <img
                src={poster}
                alt={movie.title}
                className="modal-poster"
                onError={() => setImgErr(true)}
              />
            ) : (
              <div className="modal-poster-placeholder">
                <span>🎬</span>
              </div>
            )}
          </div>

          {/* info */}
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

// ── MovieCard ────────────────────────────────────────────────
function MovieCard({ movie, index, onClick }) {
  const [imgErr, setImgErr] = useState(false);
  const img = posterUrl(movie.poster_path);

  return (
    <div
      className="card"
      onClick={() => onClick(movie)}
      style={{
        animation: "fSlide .45s ease forwards",
        animationDelay: `${index * 0.055}s`,
        opacity: 0,
      }}
    >
      <div className="card-poster">
        {img && !imgErr ? (
          <img
            src={img}
            alt={movie.title}
            className="card-poster-img"
            onError={() => setImgErr(true)}
          />
        ) : (
          <div className="card-poster-placeholder">
            <span>🎬</span>
            <span>{movie.title}</span>
          </div>
        )}

        <div className="card-badge">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="#E8C547">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
          <span>{fmt(movie.vote_average)}</span>
        </div>

        <div className="card-fade" />
      </div>

      <div className="card-meta">
        <h3 className="card-title">{movie.title}</h3>
        <span className="card-year">{year(movie.release_date)}</span>
      </div>
    </div>
  );
}

// ── Page ─────────────────────────────────────────────────────
export default function MovieSearch() {
  const [query,    setQuery]    = useState("");
  const [movies,   setMovies]   = useState([]);
  const [status,   setStatus]   = useState("idle"); // idle | loading | done | error
  const [errorMsg, setErrorMsg] = useState("");
  const [total,    setTotal]    = useState(0);
  const [selected, setSelected] = useState(null);
  const inputRef = useRef(null);

  const doSearch = useCallback(async (q) => {
    const trimmed = q.trim();
    if (!trimmed) return;
    setStatus("loading");
    setMovies([]);
    setErrorMsg("");
    try {
      const res = await fetch(`${API_BASE}/search/${encodeURIComponent(trimmed)}`);
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.Error || `HTTP ${res.status}`);
      }
      const data = await res.json();
      setMovies(data.movies      || []);
      setTotal(data.totalResults || 0);
      setStatus("done");
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  }, []);

  const handleKey   = (e) => { if (e.key === "Enter") doSearch(query); };
  const handleClear = () => {
    setQuery(""); setMovies([]); setStatus("idle"); setTotal(0);
    inputRef.current?.focus();
  };

  return (
    <div className="app">

      {/* NAV */}
      {/* <nav className="nav">
        <div className="nav-brand">
          <div className="nav-logo">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#0a0908" strokeWidth="2.5">
              <rect x="2" y="2" width="20" height="20" rx="2.5" />
              <path d="M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 7h5M17 17h5" />
            </svg>
          </div>
          <span className="nav-title">CineSearch</span>
        </div>
      </nav> */}

      <Navbar />

      <div className="main">

        {/* HERO */}
        <div className="hero">
          <p className="hero-eyebrow">powered by TMDB</p>
          <h1 className="hero-title">
            Find your<br />
            <em>next movie</em>
          </h1>
          <p className="hero-sub">Type a title — the rest is up to you</p>

          {/* SEARCH BAR */}
          <div className="search-wrap">
            <div className="search-bar">
              <div className="search-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
              </div>

              <input
                ref={inputRef}
                className="search-input"
                value={query}
                onChange={e => setQuery(e.target.value)}
                onKeyDown={handleKey}
                placeholder="e.g. Interstellar, Nolan, Parasite…"
              />

              {query && (
                <button className="search-clear" onClick={handleClear}>×</button>
              )}

              <button
                className={`search-btn ${query.trim() ? "search-btn--active" : "search-btn--inactive"}`}
                onClick={() => doSearch(query)}
                disabled={status === "loading" || !query.trim()}
              >
                {status === "loading"
                  ? <div className="spinner" />
                  : "Search"
                }
              </button>
            </div>
          </div>
        </div>

        {/* RESULTS COUNT */}
        {status === "done" && (
          <p className="results-count">
            Found  <span>{total}</span> results for „{query}"
          </p>
        )}

        {/* ERROR */}
        {status === "error" && (
          <div className="error-box">
            <p className="error-title">⚠ Request error</p>
            <p className="error-msg">{errorMsg}</p>
            <p className="error-hint">
              Make sure the backend is running at{" "}
              <code>{API_BASE}</code>
            </p>
          </div>
        )}

        {/* IDLE */}
        {status === "idle" && (
          <p className="idle-hint">↑ TYPE A TITLE AND PRESS ENTER</p>
        )}

        {/* EMPTY */}
        {status === "done" && movies.length === 0 && (
          <div className="empty-state">
            <div className="empty-icon">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.15)" strokeWidth="1.5">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
              </svg>
            </div>
            <h3 className="empty-title">No results</h3>
            <p className="empty-sub">Try a different phrase</p>
          </div>
        )}

        {/* GRID */}
        {movies.length > 0 && (
          <div className="grid">
            {movies.map((m, i) => (
              <MovieCard key={m.id} movie={m} index={i} onClick={setSelected} />
            ))}
          </div>
        )}
      </div>

      {selected && <Modal movie={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}
