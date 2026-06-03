import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, test, expect, beforeEach, afterEach, vi } from "vitest";

import Login from "../Login";

// mock Navbar
vi.mock("../Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("Login", () => {
  beforeEach(() => {
    global.fetch = vi.fn();

    // mock localStorage
    Object.defineProperty(window, "localStorage", {
      value: {
        setItem: vi.fn(),
      },
      writable: true,
    });

    // mock redirect
    delete window.location;
    window.location = { href: "" };
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  test("renders login form", () => {
    render(<Login />);

    expect(screen.getByText("Sign in")).toBeInTheDocument();
    expect(screen.getByLabelText(/e-mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i, { selector: 'input' })).toBeInTheDocument();
  });

  test("login button is disabled initially", () => {
    render(<Login />);

    expect(
      screen.getByRole("button", { name: /login/i })
    ).toBeDisabled();
  });

  test("enables login button when form is filled", async () => {
    const user = userEvent.setup();

    render(<Login />);

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "test@test.com"
    );

    await user.type(
      screen.getByLabelText(/password/i, { selector: 'input' }),
      "password123"
    );

    expect(
      screen.getByRole("button", { name: /login/i })
    ).toBeEnabled();
  });

  test("successful login", async () => {
    const user = userEvent.setup();

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        user: { id: 1, email: "test@test.com" },
      }),
    });

    render(<Login />);

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "test@test.com"
    );

    await user.type(
      screen.getByLabelText(/password/i, { selector: 'input' }),
      "password123"
    );

    await user.click(
      screen.getByRole("button", { name: /login/i })
    );

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        "http://localhost:8080/login",
        expect.objectContaining({
          method: "POST",
        })
      );
    });

    expect(localStorage.setItem).toHaveBeenCalledWith(
      "user",
      JSON.stringify({
        id: 1,
        email: "test@test.com",
      })
    );

    expect(window.location.href).toBe("/recommendations");
  });

  test("shows backend error", async () => {
    const user = userEvent.setup();

    global.fetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({
        message: "Wrong credentials",
      }),
    });

    render(<Login />);

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "bad@test.com"
    );

    await user.type(
      screen.getByLabelText(/password/i, { selector: 'input' }),
      "wrongpass"
    );

    await user.click(
      screen.getByRole("button", { name: /login/i })
    );

    await waitFor(() => {
      expect(
        screen.getByText(/wrong credentials/i)
      ).toBeInTheDocument();
    });
  });

  test("toggles password visibility", async () => {
    const user = userEvent.setup();

    render(<Login />);

    const passwordInput =
      screen.getByLabelText(/password/i, { selector: 'input' });

    expect(passwordInput).toHaveAttribute(
      "type",
      "password"
    );

    const toggleBtn = screen.getByRole("button", {
      name: /show password|hide password/i,
    });

    await user.click(toggleBtn);

    expect(passwordInput).toHaveAttribute("type", "text");
  });

  test("submits form on Enter", async () => {
    const user = userEvent.setup();

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        user: { id: 1 },
      }),
    });

    render(<Login />);

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "test@test.com"
    );

    await user.type(
      screen.getByLabelText(/password/i, { selector: 'input' }),
      "password123{enter}"
    );

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
    });
  });
});