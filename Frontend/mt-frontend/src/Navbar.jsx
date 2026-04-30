import "./Navbar.css";
import { NavLink, useNavigate } from 'react-router-dom';

export default function Navbar() {
  const navigate = useNavigate();

  const user = (() => {
    try {
      const raw = localStorage.getItem("user");
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  })();

  const handleLogout = async () => {
    try {
      await fetch("http://localhost:8080/logout", { method: "POST" });
    } catch {
      // ignoruj błąd sieciowy — i tak wylogowujemy lokalnie
    }
    localStorage.removeItem("user");
    navigate("/login");
  };

  return (
    <nav className="nav">
      <div className="nav-brand">
        <div className="nav-logo">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#0a0908" strokeWidth="2.5">
            <rect x="2" y="2" width="20" height="20" rx="2.5"/>
            <path d="M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 7h5M17 17h5"/>
          </svg>
        </div>
        <span className="nav-title">CineSearch</span>
      </div>

      <div className="nav-inner">
        <div className="nav-links">
          <NavLink
            to="/"
            className={({ isActive }) =>
              isActive ? 'nav-link nav-link--active' : 'nav-link'
            }
          >
            Search
          </NavLink>
          <NavLink
            to="/recommend"
            className={({ isActive }) =>
              isActive ? 'nav-link nav-link--active' : 'nav-link'
            }
          >
            Recommend
          </NavLink>
          <NavLink
            to="/from2recommend"
            className={({ isActive }) =>
              isActive ? 'nav-link nav-link--active' : 'nav-link'
            }
          >
            Recommend from 2
          </NavLink>
          <NavLink
            to="/rateMovie"
            className={({ isActive }) =>
              isActive ? 'nav-link nav-link--active' : 'nav-link'
            }
          >
            Rate Movie
          </NavLink>
        </div>
      </div>

      {/* USER AREA */}
      <div className="nav-user-area">
        {user ? (
          <>
            <div className="nav-user">
              <div className="nav-user-avatar">
                {user.name?.[0]?.toUpperCase() ?? "?"}
              </div>
              <span className="nav-user-name">{user.name} {user.surname}</span>
            </div>
            <button className="nav-logout-btn" onClick={handleLogout}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              Logout
            </button>
          </>
        ) : (
          <NavLink
            to="/login"
            className={({ isActive }) =>
              isActive ? 'nav-link nav-link--active' : 'nav-link'
            }
          >
            Login
          </NavLink>
        )}
      </div>
    </nav>
  );
}
