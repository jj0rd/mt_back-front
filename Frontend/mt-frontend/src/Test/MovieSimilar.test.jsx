import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, test, expect, vi, beforeEach, afterEach } from "vitest";
import MovieSimilar from "../MovieSimilar"; // Dostosuj ścieżkę do swojego projektu
import { MemoryRouter } from "react-router-dom";

// 1. Mockowanie komponentu Navbar, aby nie testować go w tym pliku
vi.mock("../Navbar", () => ({
  default: () => <div data-testid="mock-navbar">Mocked Navbar</div>,
}));

// Przykładowe dane zwracane przez API (zgodne ze strukturą z Twojego kodu)
const mockMoviesData = {
  inputMovies: ["Inception", "Interstellar"],
  movies: [
    {
      id: 550,
      title: "The Matrix",
      original_title: "The Matrix",
      poster_path: "/matrix-poster.jpg",
      backdrop_path: "/matrix-backdrop.jpg",
      release_date: "1999-03-31",
      vote_average: 8.742,
      vote_count: 24000,
      original_language: "en",
      overview: "A computer hacker learns from mysterious rebels about the true nature of his reality.",
    },
    {
      id: 157336,
      title: "Arrival",
      original_title: "Arrival",
      poster_path: null, // Testujemy przypadek braku plakatu (renderowanie placeholderu)
      release_date: "2016-11-11",
      vote_average: 7.6,
      vote_count: 16000,
      original_language: "en",
      overview: "A linguist works with the military to communicate with alien waveforms.",
    }
  ]
};

describe("MovieSimilar Component", () => {
  beforeEach(() => {
    // Rejestrujemy globalny mock dla fetch przed każdym testem
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ── STAN POCZĄTKOWY ────────────────────────────────────────────────────────
  test("renders initial idle state correctly", () => {
    render(<MovieSimilar />);

    expect(screen.getByTestId("mock-navbar")).toBeInTheDocument();
    expect(screen.getByText("Similarity Engine")).toBeInTheDocument();
    expect(screen.getByText("Your next favorites are waiting")).toBeInTheDocument();
    
    // Przycisk wyszukiwania powinien być domyślnie zablokowany (puste inputy)
    const submitBtn = screen.getByRole("button", { name: /find similar movies/i });
    expect(submitBtn).toBeDisabled();
  });

  // ── BLOKOWANIE / ODBLOKOWANIE PRZYCISKU SUBMIT ─────────────────────────────
  test("enables submit button only when both inputs are filled", async () => {
    const user = userEvent.setup();
    render(<MovieSimilar />);

    const input1 = screen.getByPlaceholderText("e.g. Inception");
    const input2 = screen.getByPlaceholderText("e.g. Interstellar");
    const submitBtn = screen.getByRole("button", { name: /find similar movies/i });

    // Wpisanie tylko do pierwszego inputu -> dalej zablokowany
    await user.type(input1, "Inception");
    expect(submitBtn).toBeDisabled();

    // Wpisanie do drugiego inputu -> odblokowany
    await user.type(input2, "Interstellar");
    expect(submitBtn).toBeEnabled();
  });

  // ── SUKCES: POBRANIE I WYŚWIETLENIE WYNIKÓW ─────────────────────────────────
  test("successfully fetches and displays similar movies", async () => {
    const user = userEvent.setup();
    
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockMoviesData,
    });

    render(
      <MemoryRouter>
        <MovieSimilar />
      </MemoryRouter>
    );

    const input1 = screen.getByPlaceholderText("e.g. Inception");
    const input2 = screen.getByPlaceholderText("e.g. Interstellar");
    const submitBtn = screen.getByRole("button", { name: /find similar movies/i });

    await user.type(input1, "Inception");
    await user.type(input2, "Interstellar");
    await user.click(submitBtn);

    // USUNIĘTO: expect(screen.getByText("Finding…")).toBeInTheDocument();
    // Ponieważ mock zwraca dane natychmiast, od razu czekamy na wyniki:

    // Oczekiwanie na pojawienie się wyników
    await waitFor(() => {
      expect(screen.getByText("The Matrix")).toBeInTheDocument();
    });

    // Sprawdzenie szczegółów wyrenderowanych kart filmów
    expect(screen.getByText("1999")).toBeInTheDocument(); 
    expect(screen.getByText("8.7")).toBeInTheDocument();  

    expect(screen.getByRole("heading", { level: 3, name: "Arrival" })).toBeInTheDocument();
    expect(screen.getByText("2016")).toBeInTheDocument();

    expect(screen.getByText("Inception")).toBeInTheDocument();
    expect(screen.getByText("Interstellar")).toBeInTheDocument();
  });

  // ── INTERAKCJA: MODAL (OTWIERANIE I ZAMYKANIE) ─────────────────────────────
  test("opens modal on card click and closes it on close button click", async () => {
    const user = userEvent.setup();
    
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockMoviesData,
    });

    render(
      <MemoryRouter>
        <MovieSimilar />
      </MemoryRouter>
    );

    await user.type(screen.getByPlaceholderText("e.g. Inception"), "Inception");
    await user.type(screen.getByPlaceholderText("e.g. Interstellar"), "Interstellar");
    await user.click(screen.getByRole("button", { name: /find similar movies/i }));

    const movieCard = await screen.findByText("The Matrix");
    await user.click(movieCard);

    const modalTitle = screen.getByRole("heading", { level: 2, name: "The Matrix" });
    expect(modalTitle).toBeInTheDocument();
    expect(screen.getByText(/A computer hacker learns from mysterious rebels/i)).toBeInTheDocument();
    
    // POPRAWKA: Używamy wyrażenia regularnego, aby spacja/przecinek nie wywaliły testu
    expect(screen.getByText(/👥.*24.*000/)).toBeInTheDocument();
    expect(screen.getByText("🌐 EN")).toBeInTheDocument();

    const closeBtn = screen.getByRole("button", { name: "×" });
    await user.click(closeBtn);

    expect(screen.queryByText(/A computer hacker learns from mysterious rebels/i)).not.toBeInTheDocument();
  });

  // ── OBSŁUGA BŁĘDU API ──────────────────────────────────────────────────────
  test("displays error message when API call fails", async () => {
    const user = userEvent.setup();
    
    // Symulacja błędu serwera z obiektem JSON zawierającym pole 'error'
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: async () => ({ error: "Database connection failed" }),
    });

    render(<MovieSimilar />);

    await user.type(screen.getByPlaceholderText("e.g. Inception"), "Inception");
    await user.type(screen.getByPlaceholderText("e.g. Interstellar"), "Interstellar");
    await user.click(screen.getByRole("button", { name: /find similar movies/i }));

    // Oczekiwanie na komunikat o błędzie
    const errorMsg = await screen.findByText("Database connection failed");
    expect(errorMsg).toBeInTheDocument();
    expect(screen.getByText("⚠ Request error")).toBeInTheDocument();
  });

  // ── SCENARIUSZ BRAKU WYNIKÓW (PUSTA LISTA) ──────────────────────────────────
  test("displays empty results message when API returns no movies", async () => {
    const user = userEvent.setup();
    
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ inputMovies: ["A", "B"], movies: [] }),
    });

    render(<MovieSimilar />);

    await user.type(screen.getByPlaceholderText("e.g. Inception"), "Movie A");
    await user.type(screen.getByPlaceholderText("e.g. Interstellar"), "Movie B");
    await user.click(screen.getByRole("button", { name: /find similar movies/i }));

    await waitFor(() => {
      expect(screen.getByText("No results found")).toBeInTheDocument();
    });
  });

  
  // ── OBSŁUGA KLAWISZA ENTER ──────────────────────────────────────────────────
  test("submits form on Enter key press when form is valid", async () => {
    const user = userEvent.setup();
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockMoviesData,
    });

    render(<MovieSimilar />);

    const input1 = screen.getByPlaceholderText("e.g. Inception");
    const input2 = screen.getByPlaceholderText("e.g. Interstellar");

    await user.type(input1, "Inception");
    await user.type(input2, "Interstellar");
    
    // Symulacja wciśnięcia klawisza Enter wewnątrz drugiego pola tekstowego
    await user.type(input2, "{Enter}");

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(screen.getByText("The Matrix")).toBeInTheDocument();
    });
  });
});