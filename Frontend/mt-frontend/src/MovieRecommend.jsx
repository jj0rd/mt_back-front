import { useState } from "react";
import "./MovieRecommend.css";
import Navbar from "./Navbar";

const TMDB_IMG = "https://image.tmdb.org/t/p/w500";
const posterUrl = (path) => (path ? `${TMDB_IMG}${path}` : null);
const year      = (date) => (date ? date.slice(0, 4) : "—");
const fmt       = (v)    => (v    ? Number(v).toFixed(1) : "N/A");

const CURRENT_YEAR = new Date().getFullYear();

// Gatunki z oficjalnymi TMDB ID (id niewidoczne dla użytkownika)
const GENRES = [
  { id: 28,    name: "Action" },
  { id: 12,    name: "Adventure" },
  { id: 16,    name: "Animation" },
  { id: 35,    name: "Comedy" },
  { id: 80,    name: "Crime" },
  { id: 99,    name: "Documentary" },
  { id: 18,    name: "Drama" },
  { id: 10751, name: "Family" },
  { id: 14,    name: "Fantasy" },
  { id: 36,    name: "History" },
  { id: 27,    name: "Horror" },
  { id: 10402, name: "Music" },
  { id: 9648,  name: "Mystery" },
  { id: 10749, name: "Romance" },
  { id: 878,   name: "Science Fiction" },
  { id: 10770, name: "TV Movie" },
  { id: 53,    name: "Thriller" },
  { id: 10752, name: "War" },
  { id: 37,    name: "Western" },
];

// ── Tag input ────────────────────────────────────────────────
function TagInput({ tags, onChange, placeholder }) {
  const [input, setInput] = useState("");

  const add = (val) => {
    const trimmed = val.trim();
    if (trimmed && !tags.includes(trimmed)) onChange([...tags, trimmed]);
    setInput("");
  };

  const handleKey = (e) => {
    if (e.key === "Enter" || e.key === ",") { e.preventDefault(); add(input); }
    else if (e.key === "Backspace" && input === "" && tags.length > 0) onChange(tags.slice(0, -1));
  };

  const remove = (tag) => onChange(tags.filter(t => t !== tag));

  return (
    <div className="tag-input-wrap">
      {tags.map(tag => (
        <span key={tag} className="tag">
          {tag}
          <button className="tag-remove" onClick={() => remove(tag)}>×</button>
        </span>
      ))}
      <input
        className="tag-input"
        value={input}
        onChange={e => setInput(e.target.value)}
        onKeyDown={handleKey}
        onBlur={() => input.trim() && add(input)}
        placeholder={tags.length === 0 ? placeholder : ""}
      />
    </div>
  );
}

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

// ── Result card ──────────────────────────────────────────────
function ResultCard({ movie, index, onClick }) {
  const [imgErr, setImgErr] = useState(false);
  const img = posterUrl(movie.poster_path);

  return (
    <div className="rcard" onClick={() => onClick(movie)} style={{ animationDelay: `${index * 0.07}s` }}>
      <div className="rcard-poster">
        {img && !imgErr ? (
          <img src={img} alt={movie.title} className="rcard-img" onError={() => setImgErr(true)} />
        ) : (
          <div className="rcard-placeholder"><span>🎬</span><span>{movie.title}</span></div>
        )}
        <div className="rcard-badge">
          <svg width="9" height="9" viewBox="0 0 24 24" fill="#E8C547">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
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

// ── Modal ────────────────────────────────────────────────────
function Modal({ movie, onClose }) {
  const [imgErr, setImgErr] = useState(false);
  const poster = posterUrl(movie.poster_path);
  const backdrop = movie.backdrop_path ? `https://image.tmdb.org/t/p/original${movie.backdrop_path}` : null;

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

// ── Page ─────────────────────────────────────────────────────
export default function MovieRecommend() {
  // genres trzyma tablicę ID (number), nazwa pobierana z GENRES[]
  const [genreIds,    setGenreIds]    = useState([]);
  const [description, setDescription] = useState("");
  const [cast,        setCast]        = useState([]);
  const [yearFrom,    setYearFrom]    = useState("");
  const [yearTo,      setYearTo]      = useState("");
  const [ratingMin,   setRatingMin]   = useState(0);
  const [tags,        setTags]        = useState([]);

  const [results,  setResults]  = useState([]);
  const [status,   setStatus]   = useState("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [selected, setSelected] = useState(null);

  const toggleGenre = (id) =>
    setGenreIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);

  // Nazwy wybranych gatunków (do zapytania i wyświetlenia tagów)
  const selectedGenreNames = GENRES
    .filter(g => genreIds.includes(g.id))
    .map(g => g.name);

    const handleSubmit = async () => {
    setStatus("loading");
    setResults([]);
    setErrorMsg("");

    // Przygotowanie danych do /discover/fuzzy
    const payload = {
      language: "en-US",
      genre: genreIds,
      description: description.trim() || null,
      peopleNames: cast.length > 0 ? cast : null,
      yearFrom: yearFrom ? parseInt(yearFrom) : null,
      yearTo: yearTo ? parseInt(yearTo) : null,
      rating: ratingMin > 0 ? ratingMin : null,
      keywords: tags.length > 0 ? tags : null,
    };

    try {
      const res = await fetch("http://localhost:8080/api/discover/fuzzy", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.Error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      const movies = data.movies || [];
      setResults(movies);
      setStatus("done");
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  };


  const handleReset = () => {
    setGenreIds([]);
    setDescription("");
    setCast([]);
    setYearFrom("");
    setYearTo("");
    setRatingMin(0);
    setTags([]);
    setResults([]);
    setStatus("idle");
  };

  const filledFields = [
    genreIds.length > 0,
    description.trim().length > 0,
    cast.length > 0,
    yearFrom || yearTo,
    ratingMin > 0,
    tags.length > 0,
  ].filter(Boolean).length;

  return (
    <div className="app">
      <Navbar />
      <div className="main">

        <div className="hero">
          <p className="hero-eyebrow">AI · TMDB</p>
          <h1 className="hero-title">Your preferences,<br /><em>perfect movies</em></h1>
          <p className="hero-sub">Set your criteria — we'll find movies tailored for you</p>
        </div>

        <div className="layout">
          <aside className="form-panel">
            <div className="form-header">
              <span className="form-header-label">Preferences</span>
              {filledFields > 0 && <span className="form-header-count">{filledFields} active</span>}
            </div>

            {/* GENRES */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>
                </svg>
                Genres
              </label>
              <div className="genre-grid">
                {GENRES.map(g => (
                  <button
                    key={g.id}
                    className={`genre-btn ${genreIds.includes(g.id) ? "genre-btn--on" : ""}`}
                    onClick={() => toggleGenre(g.id)}
                  >
                    {g.name}
                  </button>
                ))}
              </div>
            </div>

            {/* DESCRIPTION */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                Description / mood
              </label>
              <textarea
                className="field-textarea"
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="e.g. dark psychological thriller, time travel, dystopia…"
                rows={3}
              />
            </div>

            {/* CAST */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                Obsada / reżyser
              </label>
              <TagInput tags={cast} onChange={setCast} placeholder="e.g. Nolan, DiCaprio — press Enter to add" />
            </div>

            {/* YEAR RANGE */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="4" width="18" height="18" rx="2"/>
                  <line x1="16" y1="2" x2="16" y2="6"/>
                  <line x1="8" y1="2" x2="8" y2="6"/>
                  <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                Release year
              </label>
              <div className="year-row">
                <input className="field-input" type="number" min="1900" max={CURRENT_YEAR}
                  value={yearFrom} onChange={e => setYearFrom(e.target.value)} placeholder="From" />
                <span className="year-sep">—</span>
                <input className="field-input" type="number" min="1900" max={CURRENT_YEAR}
                  value={yearTo} onChange={e => setYearTo(e.target.value)} placeholder="To" />
              </div>
            </div>

            {/* RATING */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="#E8C547">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
                Minimum rating: <span className="rating-val">{ratingMin > 0 ? ratingMin.toFixed(1) : "any"}</span>
              </label>
              <div className="slider-wrap">
                <span className="slider-edge">0</span>
                <input className="slider" type="range" min="0" max="10" step="0.5"
                  value={ratingMin} onChange={e => setRatingMin(parseFloat(e.target.value))} />
                <span className="slider-edge">10</span>
              </div>
            </div>

            {/* TAGS */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                  <line x1="7" y1="7" x2="7.01" y2="7"/>
                </svg>
                Tags
              </label>
              <TagInput tags={tags} onChange={setTags} placeholder="np. slow-burn, twist ending, based on book — Enter" />
              <p className="field-hint">Free keywords describing the mood or plot</p>
            </div>

            {/* ACTIONS */}
            <div className="form-actions">
              <button className="btn-primary" onClick={handleSubmit}
                disabled={status === "loading" || filledFields === 0}>
                {status === "loading" ? (
                  <><div className="spinner" /> Searching...</>
                ) : (
                  <><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
                  </svg> Find movies</>
                )}
              </button>
              {filledFields > 0 && (
                <button className="btn-ghost" onClick={handleReset}>Clear</button>
              )}
            </div>
          </aside>

          {/* RESULTS PANEL */}
          <section className="results-panel">
            {status === "idle" && (
              <div className="results-idle">
                <div className="results-idle-icon">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="rgba(232,197,71,.25)" strokeWidth="1">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                  </svg>
                </div>
                <h3 className="results-idle-title">Set your preferences</h3>
                <p className="results-idle-sub">Fill in at least one field and click "Find movies"</p>
                <div className="results-idle-steps">
                  {["Choose a genre or describe the mood", "Add cast or tags", "Set year range and rating"].map((s, i) => (
                    <div key={i} className="step">
                      <span className="step-num">{i + 1}</span>
                      <span className="step-txt">{s}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {status === "error" && (
              <div className="error-box">
                <p className="error-title">⚠  Request error</p>
                <p className="error-msg">{errorMsg}</p>
                <p className="error-hint">Make sure the backend is running at <code>http://localhost:8080</code></p>
              </div>
            )}

            {status === "done" && results.length === 0 && (
              <div className="results-empty">
                <div className="empty-icon">
                  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.15)" strokeWidth="1.5">
                    <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
                  </svg>
                </div>
                <h3 className="empty-title">No results</h3>
                <p className="empty-sub">Try changing the criteria or expanding the year range</p>
              </div>
            )}

            {status === "done" && results.length > 0 && (
              <>
                <div className="results-header">
                  <span className="results-count">Found  <em>{results.length}</em> recommendations</span>
                  <div className="results-criteria">
                    {selectedGenreNames.map(n => <span key={n} className="crit-tag">{n}</span>)}
                    {tags.map(t => <span key={t} className="crit-tag crit-tag--alt">{t}</span>)}
                    {cast.map(c => <span key={c} className="crit-tag crit-tag--cast">{c}</span>)}
                    {(yearFrom || yearTo) && (
                      <span className="crit-tag">{yearFrom || "…"} – {yearTo || "…"}</span>
                    )}
                    {ratingMin > 0 && <span className="crit-tag">★ {ratingMin.toFixed(1)}+</span>}
                  </div>
                </div>
                <div className="results-grid">
                  {results.map((m, i) => (
                    <ResultCard key={m.id} movie={m} index={i} onClick={setSelected} />
                  ))}
                </div>
              </>
            )}
          </section>
        </div>
      </div>

      {selected && <Modal movie={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}