import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, test, expect, beforeEach, afterEach, vi } from "vitest";

import Register from "../Register";

vi.mock("../Navbar", () => ({
  default: () => <div>Navbar</div>,
}));

describe("Register", () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  test("renders registration form", () => {
    render(<Register />);

    expect(screen.getByText("Sign up")).toBeInTheDocument();

    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/e-mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  test("submit button is disabled initially", () => {
    render(<Register />);

    const submitButton = screen.getByRole("button", {
      name: /create account/i,
    });

    expect(submitButton).toBeDisabled();
  });

  test("shows password mismatch message", async () => {
    const user = userEvent.setup();

    render(<Register />);

    await user.type(
      screen.getByLabelText(/^password$/i),
      "Password123"
    );

    await user.type(
      screen.getByLabelText(/confirm password/i),
      "DifferentPassword"
    );

    expect(
      screen.getByText(/passwords do not match/i)
    ).toBeInTheDocument();
  });

  test("enables submit button when form is valid", async () => {
    const user = userEvent.setup();

    render(<Register />);

    await user.type(
      screen.getByLabelText(/first name/i),
      "John"
    );

    await user.type(
      screen.getByLabelText(/last name/i),
      "Doe"
    );

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "john@test.com"
    );

    await user.type(
      screen.getByLabelText(/^password$/i),
      "Password123"
    );

    await user.type(
      screen.getByLabelText(/confirm password/i),
      "Password123"
    );

    const submitButton = screen.getByRole("button", {
      name: /create account/i,
    });

    expect(submitButton).toBeEnabled();
  });

  test("successful registration", async () => {
    const user = userEvent.setup();

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({}),
    });

    render(<Register />);

    await user.type(
      screen.getByLabelText(/first name/i),
      "John"
    );

    await user.type(
      screen.getByLabelText(/last name/i),
      "Doe"
    );

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "john@test.com"
    );

    await user.type(
      screen.getByLabelText(/^password$/i),
      "Password123"
    );

    await user.type(
      screen.getByLabelText(/confirm password/i),
      "Password123"
    );

    await user.click(
      screen.getByRole("button", {
        name: /create account/i,
      })
    );

    await waitFor(() => {
      expect(
        screen.getByText(/account created/i)
      ).toBeInTheDocument();
    });

    expect(global.fetch).toHaveBeenCalledTimes(1);

    expect(global.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/register",
      expect.objectContaining({
        method: "POST",
      })
    );
  });

  test("shows backend error message", async () => {
    const user = userEvent.setup();

    global.fetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({
        message: "Email already exists",
      }),
    });

    render(<Register />);

    await user.type(
      screen.getByLabelText(/first name/i),
      "John"
    );

    await user.type(
      screen.getByLabelText(/last name/i),
      "Doe"
    );

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "john@test.com"
    );

    await user.type(
      screen.getByLabelText(/^password$/i),
      "Password123"
    );

    await user.type(
      screen.getByLabelText(/confirm password/i),
      "Password123"
    );

    await user.click(
      screen.getByRole("button", {
        name: /create account/i,
      })
    );

    await waitFor(() => {
      expect(
        screen.getByText(/email already exists/i)
      ).toBeInTheDocument();
    });
  });

  test("toggles password visibility", async () => {
    const user = userEvent.setup();

    render(<Register />);

    const passwordInput =
      screen.getByLabelText(/^password$/i);

    expect(passwordInput).toHaveAttribute(
      "type",
      "password"
    );

    const buttons = screen.getAllByRole("button");

    await user.click(buttons[0]);

    expect(passwordInput).toHaveAttribute(
      "type",
      "text"
    );
  });

  test("submits form when Enter is pressed", async () => {
    const user = userEvent.setup();

    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({}),
    });

    render(<Register />);

    await user.type(
      screen.getByLabelText(/first name/i),
      "John"
    );

    await user.type(
      screen.getByLabelText(/last name/i),
      "Doe"
    );

    await user.type(
      screen.getByLabelText(/e-mail/i),
      "john@test.com"
    );

    await user.type(
      screen.getByLabelText(/^password$/i),
      "Password123"
    );

    await user.type(
      screen.getByLabelText(/confirm password/i),
      "Password123{enter}"
    );

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
    });
  });
});