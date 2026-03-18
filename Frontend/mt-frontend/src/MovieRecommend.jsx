import { useState, useEffect } from "react";
import "./MovieRecommend.css";
import Navbar from "./Navbar";

const TMDB_IMG = "https://image.tmdb.org/t/p/w500";
const posterUrl = (path) => (path ? `${TMDB_IMG}${path}` : null);
const year      = (date) => (date ? date.slice(0, 4) : "—");
const fmt       = (v)    => (v    ? Number(v).toFixed(1) : "N/A");

const CURRENT_YEAR = new Date().getFullYear();

// ── Tag input ────────────────────────────────────────────────
function TagInput({ tags, onChange, placeholder }) {
  const [input, setInput] = useState("");

  const add = (val) => {
    const trimmed = val.trim();
    if (trimmed && !tags.includes(trimmed)) {
      onChange([...tags, trimmed]);
    }
    setInput("");
  };

  const handleKey = (e) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      add(input);
    } else if (e.key === "Backspace" && input === "" && tags.length > 0) {
      onChange(tags.slice(0, -1));
    }
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
    <div
      className="rcard"
      onClick={() => onClick(movie)}
      style={{ animationDelay: `${index * 0.07}s` }}
    >
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
            <p className="modal-overview">{movie.overview || "Brak opisu."}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Page ─────────────────────────────────────────────────────
export default function MovieRecommend() {
  const [genres,      setGenres]      = useState([]);
  const [availableGenres, setAvailableGenres] = useState([]);
  const [genresLoading, setGenresLoading] = useState(true);
  const [genresError, setGenresError] = useState(null);
  
  const [description, setDescription] = useState("");
  const [cast,        setCast]        = useState([]);
  const [yearFrom,    setYearFrom]    = useState("");
  const [yearTo,      setYearTo]      = useState("");
  const [ratingMin,   setRatingMin]   = useState(0);
  const [tags,        setTags]        = useState([]);

  const [results,  setResults]  = useState([]);
  const [status,   setStatus]   = useState("idle"); // idle | loading | done | error
  const [errorMsg, setErrorMsg] = useState("");
  const [selected, setSelected] = useState(null);

  // Pobierz gatunki z backendu
  useEffect(() => {
    const fetchGenres = async () => {
      try {
        setGenresLoading(true);
        const response = await fetch("http://localhost:8080/api/tmdb/movies/genres");
        
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        // Backend zwraca { genres: [...] }
        if (data && data.genres && Array.isArray(data.genres)) {
          // Wyciągnij tylko nazwy gatunków
          const genreNames = data.genres.map(g => g.name);
          setAvailableGenres(genreNames);
        } else {
          console.error("Nieprawidłowy format danych gatunków:", data);
          setAvailableGenres([]);
        }
        
        setGenresError(null);
      } catch (err) {
        console.error("Błąd pobierania gatunków:", err);
        setGenresError(err.message);
        setAvailableGenres([]);
      } finally {
        setGenresLoading(false);
      }
    };

    fetchGenres();
  }, []);

  const toggleGenre = (g) =>
    setGenres(prev => prev.includes(g) ? prev.filter(x => x !== g) : [...prev, g]);

  const handleSubmit = async () => {
    setStatus("loading");
    setResults([]);
    setErrorMsg("");

    // Zbuduj zapytanie z preferencji
    const query = [
      description.trim(),
      ...genres,
      ...cast,
      ...tags,
    ].filter(Boolean).join(" ") || "popular";

    try {
      const res = await fetch(
        `http://localhost:8080/api/tmdb/movies/search/${encodeURIComponent(query)}`
      );
      
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.Error || `HTTP ${res.status}`);
      }
      
      const data = await res.json();
      let movies = data.movies || [];

      // Filtrowanie po stronie klienta
      if (yearFrom) movies = movies.filter(m => m.release_date && parseInt(m.release_date) >= parseInt(yearFrom));
      if (yearTo)   movies = movies.filter(m => m.release_date && parseInt(m.release_date) <= parseInt(yearTo));
      if (ratingMin > 0) movies = movies.filter(m => m.vote_average >= ratingMin);

      setResults(movies);
      setStatus("done");
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  };

  const handleReset = () => {
    setGenres([]); 
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
    genres.length > 0,
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

        {/* HERO */}
        <div className="hero">
          <p className="hero-eyebrow">AI · TMDB</p>
          <h1 className="hero-title">
            Twoje preferencje,<br />
            <em>idealne filmy</em>
          </h1>
          <p className="hero-sub">Wypełnij kryteria — znajdziemy filmy skrojone pod Ciebie</p>
        </div>

        {/* FORM + RESULTS layout */}
        <div className="layout">

          {/* ── FORM PANEL ── */}
          <aside className="form-panel">
            <div className="form-header">
              <span className="form-header-label">Preferencje</span>
              {filledFields > 0 && (
                <span className="form-header-count">{filledFields} aktywne</span>
              )}
            </div>

            {/* GENRES - dynamiczne z backendu */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>
                </svg>
                Gatunki
                {genresLoading && <span className="field-loading">(ładowanie...)</span>}
                {genresError && <span className="field-error">(błąd ładowania)</span>}
              </label>
              
              {genresLoading ? (
                <div className="genres-loading">
                  <div className="spinner small" />
                  <span>Pobieranie gatunków...</span>
                </div>
              ) : genresError ? (
                <div className="genres-error">
                  <p>Nie udało się pobrać gatunków. Używam domyślnych.</p>
                  {/* Domyślne gatunki jako fallback */}
                  <div className="genre-grid">
                    {["Akcja", "Przygodowy", "Animacja", "Komedia", "Dramat"].map(g => (
                      <button
                        key={g}
                        className={`genre-btn ${genres.includes(g) ? "genre-btn--on" : ""}`}
                        onClick={() => toggleGenre(g)}
                      >{g}</button>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="genre-grid">
                  {availableGenres.map(g => (
                    <button
                      key={g}
                      className={`genre-btn ${genres.includes(g) ? "genre-btn--on" : ""}`}
                      onClick={() => toggleGenre(g)}
                    >{g}</button>
                  ))}
                </div>
              )}
            </div>

            {/* DESCRIPTION */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                Opis / klimat
              </label>
              <textarea
                className="field-textarea"
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="np. mroczny thriller psychologiczny, podróż w czasie, dystopia…"
                rows={3}
              />
            </div>

            {/* CAST */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                Obsada / reżyser
              </label>
              <TagInput
                tags={cast}
                onChange={setCast}
                placeholder="np. Nolan, DiCaprio — Enter aby dodać"
              />
            </div>

            {/* YEAR RANGE */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                Rok premiery
              </label>
              <div className="year-row">
                <input
                  className="field-input"
                  type="number"
                  min="1900"
                  max={CURRENT_YEAR}
                  value={yearFrom}
                  onChange={e => setYearFrom(e.target.value)}
                  placeholder="Od"
                />
                <span className="year-sep">—</span>
                <input
                  className="field-input"
                  type="number"
                  min="1900"
                  max={CURRENT_YEAR}
                  value={yearTo}
                  onChange={e => setYearTo(e.target.value)}
                  placeholder="Do"
                />
              </div>
            </div>

            {/* RATING */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="#E8C547">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
                Minimalna ocena: <span className="rating-val">{ratingMin > 0 ? ratingMin.toFixed(1) : "dowolna"}</span>
              </label>
              <div className="slider-wrap">
                <span className="slider-edge">0</span>
                <input
                  className="slider"
                  type="range"
                  min="0"
                  max="10"
                  step="0.5"
                  value={ratingMin}
                  onChange={e => setRatingMin(parseFloat(e.target.value))}
                />
                <span className="slider-edge">10</span>
              </div>
            </div>

            {/* TAGS */}
            <div className="field">
              <label className="field-label">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/>
                </svg>
                Tagi
              </label>
              <TagInput
                tags={tags}
                onChange={setTags}
                placeholder="np. slow-burn, twist ending, based on book — Enter"
              />
              <p className="field-hint">Wolne słowa kluczowe opisujące klimat lub fabułę</p>
            </div>

            {/* ACTIONS */}
            <div className="form-actions">
              <button
                className="btn-primary"
                onClick={handleSubmit}
                disabled={status === "loading" || filledFields === 0}
              >
                {status === "loading" ? (
                  <><div className="spinner" /> Szukam…</>
                ) : (
                  <><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg> Znajdź filmy</>
                )}
              </button>
              {filledFields > 0 && (
                <button className="btn-ghost" onClick={handleReset}>Wyczyść</button>
              )}
            </div>
          </aside>

          {/* ── RESULTS PANEL ── */}
          <section className="results-panel">

            {status === "idle" && (
              <div className="results-idle">
                <div className="results-idle-icon">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="rgba(232,197,71,.25)" strokeWidth="1">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                  </svg>
                </div>
                <h3 className="results-idle-title">Ustaw preferencje</h3>
                <p className="results-idle-sub">Wypełnij przynajmniej jedno pole i kliknij „Znajdź filmy"</p>
                <div className="results-idle-steps">
                  {["Wybierz gatunek lub wpisz opis", "Dodaj obsadę lub tagi", "Ustaw przedział lat i ocenę"].map((s, i) => (
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
                <p className="error-title">⚠ Błąd zapytania</p>
                <p className="error-msg">{errorMsg}</p>
                <p className="error-hint">Sprawdź czy backend działa na <code>http://localhost:8080</code></p>
              </div>
            )}

            {status === "done" && results.length === 0 && (
              <div className="results-empty">
                <div className="empty-icon">
                  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.15)" strokeWidth="1.5">
                    <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
                  </svg>
                </div>
                <h3 className="empty-title">Brak wyników</h3>
                <p className="empty-sub">Spróbuj zmienić kryteria lub rozszerzyć przedział lat</p>
              </div>
            )}

            {status === "done" && results.length > 0 && (
              <>
                <div className="results-header">
                  <span className="results-count">
                    Znaleziono <em>{results.length}</em> rekomendacji
                  </span>
                  <div className="results-criteria">
                    {genres.map(g => <span key={g} className="crit-tag">{g}</span>)}
                    {tags.map(t => <span key={t} className="crit-tag crit-tag--alt">{t}</span>)}
                    {cast.map(c => <span key={c} className="crit-tag crit-tag--cast">{c}</span>)}
                    {(yearFrom || yearTo) && (
                      <span className="crit-tag">{yearFrom || "…"} – {yearTo || "…"}</span>
                    )}
                    {ratingMin > 0 && (
                      <span className="crit-tag">★ {ratingMin.toFixed(1)}+</span>
                    )}
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