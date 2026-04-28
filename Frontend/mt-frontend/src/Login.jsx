import { useState } from "react";
import "./Login.css";
import Navbar from "./Navbar";

export default function Login() {
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [showPass, setShowPass] = useState(false);
  const [status, setStatus]     = useState("idle"); // idle | loading | error
  const [errorMsg, setErrorMsg] = useState("");

  const canSubmit = email.trim().length > 0 && password.length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setStatus("loading");
    setErrorMsg("");

    try {
      const res = await fetch("http://localhost:8080/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || `HTTP ${res.status}`);
      }

      // const data = await res.json();
      // np. zapisz token: localStorage.setItem("token", data.token);
      setStatus("idle");
      alert("Login successful!"); // zamień na redirect
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  };

  const handleKey = (e) => {
    if (e.key === "Enter" && canSubmit) handleSubmit();
  };

  return (
    <div className="login-app">

      {/* NAV
      <nav className="nav">
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

      <Navbar />

      {/* MAIN */}
      <main className="login-main">

        {/* LEFT — decorative */}
        <div className="login-deco" aria-hidden="true">
          <div className="deco-grid">
            {[...Array(9)].map((_, i) => (
              <div key={i} className="deco-cell" style={{ animationDelay: `${i * 0.15}s` }}>
                <span>🎬</span>
              </div>
            ))}
          </div>
          <div className="deco-overlay" />
          <div className="deco-text">
            <p className="deco-eyebrow">Welcome back</p>
            <h2 className="deco-headline">Your next<br /><em>great film</em><br />is one click away</h2>
          </div>
        </div>

        {/* RIGHT — form */}
        <div className="login-form-side">
          <div className="login-card">

            <div className="login-card-header">
              <p className="login-eyebrow">Account</p>
              <h1 className="login-title">Sign in</h1>
              <p className="login-sub">Enter your credentials to continue</p>
            </div>

            {/* EMAIL */}
            <div className="lfield">
              <label className="lfield-label" htmlFor="email">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                E-mail
              </label>
              <input
                id="email"
                className={`lfield-input${email.trim() ? " filled" : ""}`}
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={handleKey}
                placeholder="eg. jan@example.com"
                autoComplete="email"
              />
            </div>

            {/* PASSWORD */}
            <div className="lfield">
              <label className="lfield-label" htmlFor="password">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                Password
              </label>
              <div className="lfield-password-wrap">
                <input
                  id="password"
                  className={`lfield-input lfield-input--pass${password ? " filled" : ""}`}
                  type={showPass ? "text" : "password"}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  onKeyDown={handleKey}
                  placeholder="••••••••"
                  autoComplete="current-password"
                />
                <button
                  className="pass-toggle"
                  onClick={() => setShowPass(v => !v)}
                  tabIndex={-1}
                  type="button"
                  aria-label={showPass ? "Hide password" : "Show password"}
                >
                  {showPass ? (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {/* ERROR */}
            {status === "error" && (
              <div className="login-error">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#e05555" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                {errorMsg || "Wrong e-mail or password"}
              </div>
            )}

            {/* SUBMIT */}
            <button
              className="login-btn"
              onClick={handleSubmit}
              disabled={!canSubmit || status === "loading"}
            >
              {status === "loading" ? (
                <><div className="spinner" /> Login…</>
              ) : (
                <>
                  Login
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <line x1="5" y1="12" x2="19" y2="12"/>
                    <polyline points="12 5 19 12 12 19"/>
                  </svg>
                </>
              )}
            </button>

            {/* REGISTER LINK */}
            <p className="login-register-hint">
              Don't hava an account?{" "}
              <a href="/register" className="login-register-link">Register</a>
            </p>

          </div>
        </div>
      </main>
    </div>
  );
}
