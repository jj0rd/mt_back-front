import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, test, expect, beforeEach, afterEach, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Navbar from "../Navbar";

// Tworzymy mock dla useNavigate, aby śledzić przekierowania
const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe("Navbar", () => {
  beforeEach(() => {
    global.fetch = vi.fn();

    // Czyszczenie i mockowanie localStorage
    localStorage.clear();
    vi.spyOn(Storage.prototype, "setItem");
    vi.spyOn(Storage.prototype, "getItem");
    vi.spyOn(Storage.prototype, "removeItem");
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // Pomocnicza funkcja do renderowania z Routerem
  const renderNavbar = (initialEntries = ["/"]) => {
    return render(
      <MemoryRouter initialEntries={initialEntries}>
        <Navbar />
      </MemoryRouter>
    );
  };

  test("renders brand title and all navigation links", () => {
    renderNavbar();

    expect(screen.getByText("CineSearch")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^search$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^recommend$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /recommend from 2/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /rate movie/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /recommend my taste/i })).toBeInTheDocument();
  });

  test("shows Login link when user is not logged in", () => {
    renderNavbar();

    expect(screen.getByRole("link", { name: /^login$/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /logout/i })).not.toBeInTheDocument();
  });

  test("shows user details and logout button when user is logged in", () => {
    const fakeUser = { name: "Jan", surname: "Kowalski" };
    localStorage.setItem("user", JSON.stringify(fakeUser));

    renderNavbar();

    // Sprawdzenie awatara (pierwsza litera imienia)
    expect(screen.getByText("J")).toBeInTheDocument();
    // Sprawdzenie pełnego imienia i nazwiska
    expect(screen.getByText("Jan Kowalski")).toBeInTheDocument();
    // Przycisk wylogowania powinien być widoczny
    expect(screen.getByRole("button", { name: /logout/i })).toBeInTheDocument();
    // Link do logowania powinien zniknąć
    expect(screen.queryByRole("link", { name: /^login$/i })).not.toBeInTheDocument();
  });

  test("handles broken/invalid JSON in localStorage gracefully", () => {
    localStorage.setItem("user", "{invalid-json");

    renderNavbar();

    // Powinien potraktować użytkownika jako niezalogowanego
    expect(screen.getByRole("link", { name: /^login$/i })).toBeInTheDocument();
  });

  test("successful logout calls backend API, clears storage and redirects", async () => {
    const user = userEvent.setup();
    const fakeUser = { name: "Jan", surname: "Kowalski" };
    localStorage.setItem("user", JSON.stringify(fakeUser));

    global.fetch.mockResolvedValueOnce({ ok: true });

    renderNavbar();

    const logoutBtn = screen.getByRole("button", { name: /logout/i });
    await user.click(logoutBtn);

    // Sprawdzamy czy strzał do API się odbył
    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/logout",
      { method: "POST" }
    );

    // Sprawdzamy czyszczenie danych lokalnych i nawigację
    expect(localStorage.removeItem).toHaveBeenCalledWith("user");
    expect(mockNavigate).toHaveBeenCalledWith("/login");
  });

  test("logout works and redirects even if backend API fails", async () => {
    const user = userEvent.setup();
    const fakeUser = { name: "Jan", surname: "Kowalski" };
    localStorage.setItem("user", JSON.stringify(fakeUser));

    // Symulujemy błąd sieciowy
    global.fetch.mockRejectedValueOnce(new Error("Network error"));

    renderNavbar();

    const logoutBtn = screen.getByRole("button", { name: /logout/i });
    await user.click(logoutBtn);

    // Mimo błędu czyszczenie danych i przekierowanie musi się udać
    expect(localStorage.removeItem).toHaveBeenCalledWith("user");
    expect(mockNavigate).toHaveBeenCalledWith("/login");
  });

  test("applies active class to the current NavLink", () => {
    // Renderujemy na ścieżce "/recommend"
    renderNavbar(["/recommend"]);

    const recommendLink = screen.getByRole("link", { name: /^recommend$/i });
    const searchLink = screen.getByRole("link", { name: /^search$/i });

    // Link aktywnej ścieżki powinien mieć klasę --active
    expect(recommendLink).toHaveClass("nav-link--active");
    // Inny link nie powinien jej mieć
    expect(searchLink).not.toHaveClass("nav-link--active");
  });
});