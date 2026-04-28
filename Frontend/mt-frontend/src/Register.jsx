import { useState } from "react";
import "./Register.css";
import Navbar from "./Navbar";

export default function Register() {
  const [form, setForm] = useState({
    name: "",
    surname: "",
    email: "",
    password: "",
    confirm: "",
  });
  const [showPass, setShowPass]       = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [status, setStatus]           = useState("idle"); // idle | loading | error | success
  const [errorMsg, setErrorMsg]       = useState("");

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const passwordsMatch = form.password === form.confirm;
  const passwordStrength = (() => {
    const p = form.password;
    if (!p) return 0;
    let s = 0;
    if (p.length >= 8)          s++;
    if (/[A-Z]/.test(p))        s++;
    if (/[0-9]/.test(p))        s++;
    if (/[^A-Za-z0-9]/.test(p)) s++;
    return s;
  })();

  const strengthLabel = ["", "Weak", "Fair", "Good", "Strong"][passwordStrength];
  const strengthClass = ["", "weak", "fair", "good", "strong"][passwordStrength];

  const canSubmit =
    form.name.trim().length > 0 &&
    form.surname.trim().length > 0 &&
    form.email.trim().length > 0 &&
    form.password.length >= 6 &&
    passwordsMatch &&
    status !== "loading";

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setStatus("loading");
    setErrorMsg("");

    try {
      const res = await fetch("http://localhost:8080/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name:     form.name.trim(),
          surname:  form.surname.trim(),
          email:    form.email.trim(),
          password: form.password,
        }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || body.error || `HTTP ${res.status}`);
      }

      setStatus("success");
    } catch (err) {
      setErrorMsg(err.message);
      setStatus("error");
    }
  };

  const handleKey = (e) => {
    if (e.key === "Enter" && canSubmit) handleSubmit();
  };

  if (status === "success") {
    return (
      <div className="register-app">
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
        </nav>
        <div className="success-screen">
          <div className="success-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#E8C547" strokeWidth="2">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <h2 className="success-title">Account created!</h2>
          <p className="success-sub">Welcome to CineMatch, <em>{form.name}</em>.</p>
          <a href="/login" className="success-btn">Go to Sign In →</a>
        </div>
      </div>
    );
  }

  return (
    <div className="register-app">

      <Navbar />

      <main className="register-main">

        {/* LEFT — decorative */}
        <div className="reg-deco" aria-hidden="true">
          <div className="deco-grid">
            {[...Array(9)].map((_, i) => (
              <div key={i} className="deco-cell" style={{ animationDelay: `${i * 0.12}s` }}>
                <span>🎬</span>
              </div>
            ))}
          </div>
          <div className="deco-overlay" />
          <div className="deco-text">
            <p className="deco-eyebrow">New here?</p>
            <h2 className="deco-headline">Join a world<br />of <em>cinematic</em><br />discoveries</h2>
            <div className="deco-features">
              {[
                { icon: "✦", label: "AI-powered recommendations" },
                { icon: "✦", label: "Personalized to your taste" },
                { icon: "✦", label: "Powered by TMDB" },
              ].map((f, i) => (
                <div key={i} className="deco-feature" style={{ animationDelay: `${0.4 + i * 0.12}s` }}>
                  <span className="deco-feature-icon">{f.icon}</span>
                  <span className="deco-feature-label">{f.label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* RIGHT — form */}
        <div className="reg-form-side">
          <div className="reg-card">

            <div className="reg-card-header">
              <p className="reg-eyebrow">Create account</p>
              <h1 className="reg-title">Sign up</h1>
              <p className="reg-sub">Fill in the details below to get started</p>
            </div>

            {/* NAME + SURNAME */}
            <div className="rfield-row">
              <div className="rfield">
                <label className="rfield-label" htmlFor="name">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  First name
                </label>
                <input
                  id="name"
                  className={`rfield-input${form.name.trim() ? " filled" : ""}`}
                  type="text"
                  value={form.name}
                  onChange={set("name")}
                  onKeyDown={handleKey}
                  placeholder="e.g. John"
                  autoComplete="given-name"
                />
              </div>
              <div className="rfield">
                <label className="rfield-label" htmlFor="surname">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  Last name
                </label>
                <input
                  id="surname"
                  className={`rfield-input${form.surname.trim() ? " filled" : ""}`}
                  type="text"
                  value={form.surname}
                  onChange={set("surname")}
                  onKeyDown={handleKey}
                  placeholder="e.g. Doe"
                  autoComplete="family-name"
                />
              </div>
            </div>

            {/* EMAIL */}
            <div className="rfield">
              <label className="rfield-label" htmlFor="email">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                E-mail
              </label>
              <input
                id="email"
                className={`rfield-input${form.email.trim() ? " filled" : ""}`}
                type="email"
                value={form.email}
                onChange={set("email")}
                onKeyDown={handleKey}
                placeholder="e.g. john@example.com"
                autoComplete="email"
              />
            </div>

            {/* PASSWORD */}
            <div className="rfield">
              <label className="rfield-label" htmlFor="password">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                Password
              </label>
              <div className="rfield-pass-wrap">
                <input
                  id="password"
                  className={`rfield-input rfield-input--pass${form.password ? " filled" : ""}`}
                  type={showPass ? "text" : "password"}
                  value={form.password}
                  onChange={set("password")}
                  onKeyDown={handleKey}
                  placeholder="min. 6 characters"
                  autoComplete="new-password"
                />
                <button className="pass-toggle" onClick={() => setShowPass(v => !v)} tabIndex={-1} type="button">
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

              {/* Strength bar */}
              {form.password.length > 0 && (
                <div className="strength-wrap">
                  <div className="strength-bar">
                    {[1, 2, 3, 4].map(n => (
                      <div key={n} className={`strength-seg${passwordStrength >= n ? ` strength-seg--${strengthClass}` : ""}`} />
                    ))}
                  </div>
                  <span className={`strength-label strength-label--${strengthClass}`}>{strengthLabel}</span>
                </div>
              )}
            </div>

            {/* CONFIRM PASSWORD */}
            <div className="rfield">
              <label className="rfield-label" htmlFor="confirm">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                Confirm password
              </label>
              <div className="rfield-pass-wrap">
                <input
                  id="confirm"
                  className={`rfield-input rfield-input--pass${
                    form.confirm
                      ? passwordsMatch ? " filled" : " error"
                      : ""
                  }`}
                  type={showConfirm ? "text" : "password"}
                  value={form.confirm}
                  onChange={set("confirm")}
                  onKeyDown={handleKey}
                  placeholder="repeat password"
                  autoComplete="new-password"
                />
                <button className="pass-toggle" onClick={() => setShowConfirm(v => !v)} tabIndex={-1} type="button">
                  {showConfirm ? (
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
              {form.confirm && !passwordsMatch && (
                <p className="rfield-mismatch">Passwords do not match</p>
              )}
            </div>

            {/* ERROR */}
            {status === "error" && (
              <div className="reg-error">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#e05555" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="12"/>
                  <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                {errorMsg || "Registration failed. Please try again."}
              </div>
            )}

            {/* SUBMIT */}
            <button
              className="reg-btn"
              onClick={handleSubmit}
              disabled={!canSubmit}
            >
              {status === "loading" ? (
                <><div className="spinner" /> Creating account…</>
              ) : (
                <>
                  Create account
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <line x1="5" y1="12" x2="19" y2="12"/>
                    <polyline points="12 5 19 12 12 19"/>
                  </svg>
                </>
              )}
            </button>

            {/* LOGIN LINK */}
            <p className="reg-login-hint">
              Already have an account?{" "}
              <a href="/login" className="reg-login-link">Sign in</a>
            </p>

          </div>
        </div>
      </main>
    </div>
  );
}
