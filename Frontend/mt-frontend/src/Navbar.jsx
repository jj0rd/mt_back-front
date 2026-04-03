import "./Navbar.css";
import { NavLink } from 'react-router-dom';

export default function Navbar() {
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
        </div>
      </div>
    </nav>
  );
}