import { render, screen, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, test, expect, beforeEach, afterEach, vi } from "vitest";

import MovieSearch from "../MovieSearch";

// 1. Mockujemy komponent Navbar
vi.mock("../Navbar", () => ({
  default: () => <div data-testid="mock-navbar">Mocked Navbar</div>,
}));

const fakeMovie = {
  id: 101,
  title: "Interstellar",
  original_title: "Interstellar Space",
  poster_path: "/path-to-poster.jpg",
  backdrop_path: "/path-to-backdrop.jpg",
  release_date: "2014-11-07",
  original_language: "en",
  vote_average: 8.642,
  vote_count: 34000,
  overview: "A team of explorers travel through a wormhole in space.",
};

const mockApiResponse = {
  movies: [fakeMovie],
  totalResults: 1,
};

describe("MovieSearch Component", () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  test("renders initial idle state correctly", () => {
    render(<MovieSearch />);

    expect(screen.getByTestId("mock-navbar")).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e.g. Interstellar/i)).toBeInTheDocument();
    expect(screen.getByText(/TYPE A TITLE AND PRESS ENTER/i)).toBeInTheDocument();
    
    const searchButton = screen.getByRole("button", { name: /^search$/i });
    expect(searchButton).toBeDisabled();
  });

  test("performs a successful search and renders movie cards", async () => {
    const user = userEvent.setup();
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponse,
    });

    render(<MovieSearch />);

    const input = screen.getByPlaceholderText(/e.g. Interstellar/i);
    const searchButton = screen.getByRole("button", { name: /^search$/i });

    await user.type(input, "Interstellar");
    expect(searchButton).not.toBeDisabled();
    await user.click(searchButton);

    // Sukces wyszukiwania potwierdzamy czekając na wyrenderowanie karty z tytułem filmu
    const movieHeading = await screen.findByRole("heading", { name: "Interstellar", level: 3 });
    expect(movieHeading).toBeInTheDocument();

    // Weryfikacja endpointu API
    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/tmdb/movies/search/Interstellar"
    );

    // Dodatkowe asercje potwierdzające poprawne dane na karcie filmu
    expect(screen.getByText("2014")).toBeInTheDocument();
    expect(screen.getByText("8.6")).toBeInTheDocument();
  });

  test("displays empty state when no movies are found", async () => {
    const user = userEvent.setup();
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ movies: [], totalResults: 0 }),
    });

    render(<MovieSearch />);

    const input = screen.getByPlaceholderText(/e.g. Interstellar/i);
    await user.type(input, "NonExistentMovie");
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(screen.getByText("No results")).toBeInTheDocument();
      expect(screen.getByText("Try a different phrase")).toBeInTheDocument();
    });
  });

  test("displays error message when API call fails", async () => {
    const user = userEvent.setup();
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: async () => ({ Error: "Internal server error on TMDB" }),
    });

    render(<MovieSearch />);

    const input = screen.getByPlaceholderText(/e.g. Interstellar/i);
    await user.type(input, "ErrorMovie");
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(screen.getByText(/Request error/i)).toBeInTheDocument();
      expect(screen.getByText("Internal server error on TMDB")).toBeInTheDocument();
    });
  });

  test("clears the search input and results on clear button click", async () => {
    const user = userEvent.setup();
    render(<MovieSearch />);

    const input = screen.getByPlaceholderText(/e.g. Interstellar/i);
    await user.type(input, "Inception");

    const clearButton = screen.getByRole("button", { name: "×" });
    expect(clearButton).toBeInTheDocument();

    await user.click(clearButton);

    expect(input).toHaveValue("");
    expect(input).toHaveFocus();
    expect(screen.getByText(/TYPE A TITLE AND PRESS ENTER/i)).toBeInTheDocument();
  });

  test("opens modal on movie card click and closes it via overlay or close button", async () => {
    const user = userEvent.setup();
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponse,
    });

    render(<MovieSearch />);

    const input = screen.getByPlaceholderText(/e.g. Interstellar/i);
    await user.type(input, "Interstellar");
    await user.keyboard("{Enter}");

    const movieCard = await screen.findByText("Interstellar");
    await user.click(movieCard);

    const modalTitle = screen.getByRole("heading", { name: "Interstellar", level: 2 });
    expect(modalTitle).toBeInTheDocument();
    expect(screen.getByText("Interstellar Space")).toBeInTheDocument();
    expect(screen.getByText("📅 2014")).toBeInTheDocument();
    expect(screen.getByText("🌐 EN")).toBeInTheDocument();
    
    expect(screen.getByText(/👥\s*34[\s,.]000/)).toBeInTheDocument();
    expect(screen.getByText(fakeMovie.overview)).toBeInTheDocument();

    // FIX: Pobieramy przycisk "×", ignorując przycisk czyszczenia inputu (używamy dokładnego selektora klasy modala)
    const closeButton = document.querySelector(".modal-close");
    expect(closeButton).toBeInTheDocument();
    await user.click(closeButton);

    expect(screen.queryByRole("heading", { name: "Interstellar", level: 2 })).not.toBeInTheDocument();
  });

  test("fallback placeholders are used when images fail to load", async () => {
    const user = userEvent.setup();
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponse,
    });

    render(<MovieSearch />);

    const input = screen.getByPlaceholderText(/e.g. Interstellar/i);
    await user.type(input, "Interstellar");
    await user.keyboard("{Enter}");

    const img = await screen.findByRole("img", { name: "Interstellar" });
    
    // FIX: Wywołanie opakowane w act(), ponieważ bezpośrednio wywołuje zmianę stanu komponentu (setImgErr)
    act(() => {
      img.dispatchEvent(new Event("error"));
    });

    // FIX: Czekamy asynchronicznie na zniknięcie obrazka i render struktury alternatywnej
    await waitFor(() => {
      expect(img).not.toBeInTheDocument();
      expect(screen.getByText("🎬")).toBeInTheDocument();
    });
  });
});