import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, test, expect, beforeEach, afterEach, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import RateMovie from "../RateMovie";

vi.mock("../Navbar", () => ({
  default: () => <div data-testid="mock-navbar">Mocked Navbar</div>,
}));

const mockMovie = {
  id: 42,
  title: "Inception",
  overview: "A thief who steals corporate secrets through the use of dream-sharing technology...",
  posterPath: "https://image.tmdb.org/t/p/w500/inception.jpg",
  releaseYear: "2010",
  rating: 8.8,
  genres: ["Sci-Fi", "Action"],
};

describe("RateMovie Component", () => {
  beforeEach(() => {
    localStorage.clear();
    // Używamy bezpiecznego szpiega Vitest zamiast bezpośredniego nadpisywania vi.fn()
    vi.spyOn(global, "fetch");
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers(); // Bezpieczne resetowanie timerów po każdym teście
  });

  // ── 1. TEST: BRAK AUTORYZACJI (PASSED) ──────────────────────────────────
  test("renders sign-in wall when user is not logged in", () => {
    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );

    expect(screen.getByTestId("mock-navbar")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Sign in to rate movies/i })).toBeInTheDocument();
  });

  // ── 2. TEST: POBIERANIE I WYŚWIETLANIE (PASSED) ─────────────────────────
  test("fetches and displays movie details when logged in", async () => {
    localStorage.setItem("user", JSON.stringify({ id: "user-123" }));
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockMovie,
    });

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );

    const title = await screen.findByRole("heading", { name: "Inception", level: 2 });
    expect(title).toBeInTheDocument();
    expect(screen.getByText(mockMovie.overview)).toBeInTheDocument();
  });

  // ── 3. TEST: INTERAKCJA Z GWIAZDKAMI (PASSED) ────────────────────────────
  test("handles star ratings hover effects and selection", async () => {
    localStorage.setItem("user", JSON.stringify({ id: "user-123" }));
    global.fetch.mockResolvedValueOnce({ ok: true, json: async () => mockMovie });

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );
    
    await screen.findByRole("heading", { name: "Inception", level: 2 });
    const stars = screen.getAllByRole("button", { name: /Rate \d out of 5/i });

    await user.click(stars[3]);
    expect(stars[3].className).toContain("star-btn--on");
  });

  // ── 4. TEST: ZAPIS OCENY (STABLE VERSION) ────────────────────────────────────────
  test("successfully submits rating and automatically fetches next movie", async () => {
    // Rezygnujemy z vi.useFakeTimers() – pozwalamy testowi działać w czasie rzeczywistym
    localStorage.setItem("user", JSON.stringify({ id: "user-123" })); 
    
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: async () => mockMovie }) // Pierwszy render (Inception)
      .mockResolvedValueOnce({ ok: true, json: async () => ({ status: "success" }) }) // Zapis POST
      .mockResolvedValueOnce({ ok: true, json: async () => ({ ...mockMovie, title: "Interstellar", id: 99 }) }); // Następny render

    // Czysty userEvent bez żadnych nakładek na timery
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );
    
    // Czekamy na załadowanie pierwszego filmu
    await screen.findByRole("heading", { name: "Inception", level: 2 });

    // Klikamy gwiazdkę
    const stars = screen.getAllByRole("button", { name: /Rate \d out of 5/i });
    await user.click(stars[4]);

    // Klikamy zapisz
    const saveButton = screen.getByRole("button", { name: /Save rating/i });
    await user.click(saveButton);

    // Dajemy Reactowi i fetchowi czas na przetworzenie kolejnych kroków i strzał po nowy film.
    // findBy ma domyślny timeout 1000ms, co w zupełności wystarczy na asynchroniczne przejście.
    const nextTitle = await screen.findByRole("heading", { name: "Interstellar", level: 2 }, { timeout: 2000 });
    expect(nextTitle).toBeInTheDocument();
  });

  // ── 5. TEST: POMIJANIE FILMU (FIXED) ───────────────────────────────────
  test("increments skipped badge count and loads next movie on skip click", async () => {
    localStorage.setItem("user", JSON.stringify({ id: "user-123" }));
    
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: async () => mockMovie })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ ...mockMovie, title: "The Dark Knight" }) });

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );
    
    await screen.findByRole("heading", { name: "Inception", level: 2 });

    const skipButton = screen.getByRole("button", { name: /Haven't seen it/i });
    await user.click(skipButton);

    const nextTitle = await screen.findByRole("heading", { name: "The Dark Knight", level: 2 });
    expect(nextTitle).toBeInTheDocument();
  });

  // ── 6. TEST: SCENARIUSZ BRAKU FILMÓW (FIXED) ─────────────────────────────
  test("displays all caught up message when API returns 204 or 404", async () => {
    localStorage.setItem("user", JSON.stringify({ id: "user-123" }));
    global.fetch.mockResolvedValueOnce({ status: 204, ok: true });

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );

    const caughtUpHeader = await screen.findByRole("heading", { name: /All caught up!/i });
    expect(caughtUpHeader).toBeInTheDocument();
  });

  // ── 7. TEST: BŁĄD POBIERANIA I RETRY (FIXED) ────────────────────────────
  test("displays error screen on fetch failure and retries fetching", async () => {
    localStorage.setItem("user", JSON.stringify({ id: "user-123" }));
    
    global.fetch
      .mockRejectedValueOnce(new Error("Network Error"))
      .mockResolvedValueOnce({ ok: true, json: async () => mockMovie });

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );

    const errorText = await screen.findByText(/Failed to load movie/i);
    expect(errorText).toBeInTheDocument();

    const tryAgainBtn = screen.getByRole("button", { name: /Try again/i });
    await user.click(tryAgainBtn);

    const movieTitle = await screen.findByRole("heading", { name: "Inception", level: 2 });
    expect(movieTitle).toBeInTheDocument();
  });

  // ── 8. TEST: BŁĄD PODCZAS ZAPISYWANIA OCENY (FIXED) ─────────────────────
  test("displays custom error message from server when saving fails", async () => {
    localStorage.setItem("user", JSON.stringify({ id: "user-123" }));
    
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: async () => mockMovie })
      .mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({ message: "Rating is outside allowed range" }),
      });

    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <RateMovie />
      </MemoryRouter>
    );
    
    await screen.findByRole("heading", { name: "Inception", level: 2 });

    const stars = screen.getAllByRole("button", { name: /Rate \d out of 5/i });
    await user.click(stars[0]);

    const saveButton = screen.getByRole("button", { name: /Save rating/i });
    await user.click(saveButton);

    const serverError = await screen.findByText("Rating is outside allowed range");
    expect(serverError).toBeInTheDocument();
  });
});