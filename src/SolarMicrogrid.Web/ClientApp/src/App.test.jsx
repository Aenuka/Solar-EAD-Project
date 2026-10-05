import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";

const base = {
  controller: "Home",
  page: "Index",
  model: {},
  meta: {},
  errors: {},
  notices: {},
  csrfToken: "csrf-test",
  user: { id: "admin", name: "Solar Admin", role: "Backoffice" },
};
const station = {
  id: "station-1",
  name: "Coastal Solar",
  address: "10 Coast Road",
  latitude: 6.9,
  longitude: 79.8,
  capacityKw: 50,
  storageKwh: 100,
  batterySlots: 4,
  active: true,
  activeReservations: 0,
  version: 7,
  schedule: {
    days: [1, 2],
    opensAt: "08:00",
    closesAt: "18:00",
    timeZone: "Asia/Colombo",
  },
  slots: [],
};
const response = (data, url) => ({
  headers: new Headers({ "content-type": "application/json" }),
  json: async () => data,
  redirected: Boolean(url),
  url,
});

describe("React portal workflows", () => {
  it("renders the operator dashboard and submits pending approval with antiforgery", async () => {
    const user = userEvent.setup();
    const booking = {
      id: "reservation-1",
      reservationId: "RES-001",
      prosumerNic: "199012345678",
      stationId: "station-1",
      reservationDate: "2026-10-05T10:00:00Z",
      energyAmountKwh: 12,
    };
    const initial = {
      ...base,
      controller: "Operator",
      page: "Dashboard",
      user: { ...base.user, role: "GridOperator" },
      meta: {
        PendingCount: 1,
        ApprovedFutureCount: 2,
        CompletedCount: 1,
        PendingReservations: [booking],
        RecentCompleted: [
          {
            ...booking,
            id: "reservation-2",
            reservationId: "RES-002",
            completedAt: "2026-10-05T09:00:00Z",
          },
        ],
      },
    };
    const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      response(
        {
          ...initial,
          controller: "Bookings",
          page: "Pending",
          model: [],
        },
        "http://localhost/Bookings/Pending",
      ),
    );
    render(<App initialData={initial} />);
    expect(
      screen.getByRole("heading", { name: "Operator Dashboard" }),
    ).toBeVisible();
    expect(screen.getByText("RES-001")).toBeVisible();
    expect(screen.getByText("RES-002")).toBeVisible();
    expect(screen.getByRole("link", { name: "Overview" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    await user.click(screen.getByRole("button", { name: "Approve" }));
    await screen.findByRole("heading", { name: "Pending reservations" });
    expect(fetch.mock.calls[0][0]).toBe("/Bookings/Approve/reservation-1");
    expect(fetch.mock.calls[0][1].body.get("__RequestVerificationToken")).toBe(
      "csrf-test",
    );
  });

  it("submits login with the antiforgery token and renders a server redirect", async () => {
    const user = userEvent.setup();
    const fetch = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(response(base, "http://localhost/Home"));
    render(
      <App
        initialData={{
          ...base,
          user: null,
          controller: "Account",
          page: "Login",
        }}
      />,
    );
    await user.type(screen.getByLabelText("Username"), "operator");
    await user.type(screen.getByLabelText("Password"), "correct-password");
    await user.click(screen.getByRole("button", { name: /Sign in/ }));
    await screen.findByText("A brighter overview.");
    expect(fetch.mock.calls[0][0]).toBe("/Account/Login");
    const options = fetch.mock.calls[0][1];
    expect(options.body.get("Username")).toBe("operator");
    expect(options.body.get("__RequestVerificationToken")).toBe("csrf-test");
    expect(options.headers.Accept).toBe("application/json");
    expect(options.credentials).toBe("same-origin");
  });

  it("renders validation errors and retained values without restoring passwords", async () => {
    const user = userEvent.setup();
    const initial = {
      ...base,
      controller: "Staff",
      page: "Create",
      model: {
        fullName: "New Member",
        username: "new_member",
        email: "new@example.test",
        role: "GridOperator",
      },
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      response({
        ...initial,
        errors: { Username: ["Username is already in use."] },
      }),
    );
    render(<App initialData={initial} />);
    await user.type(
      screen.getByLabelText("Temporary password"),
      "long-valid-password",
    );
    await user.click(screen.getByRole("button", { name: "Create account" }));
    await screen.findByRole("alert");
    expect(screen.getByLabelText("Full name")).toHaveValue("New Member");
    expect(screen.getByLabelText("Temporary password")).toHaveValue("");
    expect(screen.getByLabelText("Username")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("keeps administrator actions out of the operator station screen", () => {
    render(
      <App
        initialData={{
          ...base,
          controller: "Stations",
          page: "Details",
          model: station,
          user: { ...base.user, role: "GridOperator" },
        }}
      />,
    );
    expect(screen.getByRole("button", { name: "Save schedule" })).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Deactivate station" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Edit station" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "Team & access" }),
    ).not.toBeInTheDocument();
  });

  it("submits repeated schedule days and the current station version", async () => {
    const user = userEvent.setup();
    const initial = {
      ...base,
      controller: "Stations",
      page: "Details",
      model: station,
    };
    const fetch = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(response(initial));
    render(<App initialData={initial} />);
    await user.click(screen.getByLabelText("Wed"));
    await user.click(screen.getByRole("button", { name: "Save schedule" }));
    await waitFor(() => expect(fetch).toHaveBeenCalledOnce());
    expect(fetch.mock.calls[0][0]).toBe("/Stations/Schedule/station-1");
    expect(fetch.mock.calls[0][1].body.getAll("Days")).toEqual(["1", "2", "3"]);
    expect(fetch.mock.calls[0][1].body.get("Version")).toBe("7");
  });

  it("requires confirmation before station deactivation", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const fetch = vi.spyOn(globalThis, "fetch");
    render(
      <App
        initialData={{
          ...base,
          controller: "Stations",
          page: "Details",
          model: station,
        }}
      />,
    );
    await user.click(
      screen.getByRole("button", { name: "Deactivate station" }),
    );
    expect(window.confirm).toHaveBeenCalledOnce();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("preserves protected staff access values without editable role controls", () => {
    render(
      <App
        initialData={{
          ...base,
          controller: "Staff",
          page: "Edit",
          model: {
            id: "admin",
            username: "admin",
            fullName: "Admin",
            email: "admin@example.test",
            role: "Backoffice",
            status: "Active",
            version: 3,
          },
        }}
      />,
    );
    expect(
      screen.queryByRole("combobox", { name: "Role" }),
    ).not.toBeInTheDocument();
    expect(document.querySelector("input[name=Role]")).toHaveValue(
      "Backoffice",
    );
    expect(document.querySelector("input[name=Version]")).toHaveValue("3");
  });

  it("posts booking approvals to the existing controller and refreshes pending rows", async () => {
    const user = userEvent.setup();
    const initial = {
      ...base,
      controller: "Bookings",
      page: "Pending",
      model: [
        {
          id: "b1",
          reservationId: "RES-1",
          prosumerNic: "199012301234",
          stationId: "s1",
          slotId: "w1",
          reservationDate: "2026-10-01T08:00:00Z",
          energyAmountKwh: 5,
          status: "PENDING",
        },
      ],
    };
    const fetch = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(
        response(
          { ...initial, model: [] },
          "http://localhost/Bookings/Pending",
        ),
      );
    render(<App initialData={initial} />);
    await user.click(screen.getByRole("button", { name: "Approve" }));
    await screen.findByText("All caught up. No pending reservations.");
    expect(fetch.mock.calls[0][0]).toBe("/Bookings/Approve/b1");
  });

  it("keeps entered values when a network request fails and allows retry", async () => {
    const user = userEvent.setup();
    vi.spyOn(globalThis, "fetch").mockRejectedValue(
      new Error("Network unavailable"),
    );
    render(
      <App
        initialData={{
          ...base,
          user: null,
          controller: "Account",
          page: "Login",
        }}
      />,
    );
    await user.type(screen.getByLabelText("Username"), "operator");
    await user.type(screen.getByLabelText("Password"), "password-value");
    await user.click(screen.getByRole("button", { name: /Sign in/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Network unavailable",
    );
    expect(screen.getByLabelText("Username")).toHaveValue("operator");
    expect(screen.getByRole("button", { name: /Sign in/ })).toBeEnabled();
  });

  it("submits prosumer decisions with the request and account version", async () => {
    const user = userEvent.setup();
    const account = {
      nic: "199012301234",
      fullName: "Test Prosumer",
      email: "solar@example.test",
      phone: "+94771234567",
      address: "10 Solar Road",
      status: "Active",
      version: 5,
      createdAt: "2026-01-01T00:00:00Z",
      recentEvents: [],
      deactivationRequest: {
        id: "request-1",
        status: "Pending",
        reason: "Moving home",
        requestedAt: "2026-09-01T00:00:00Z",
      },
    };
    const initial = {
      ...base,
      controller: "Prosumers",
      page: "Details",
      model: { ...account, account },
    };
    const fetch = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(response(initial));
    render(<App initialData={initial} />);
    await user.selectOptions(screen.getByLabelText("Decision"), "Rejected");
    await user.type(
      screen.getByLabelText("Review note"),
      "Member wishes to stay connected.",
    );
    await user.click(screen.getByRole("button", { name: "Submit decision" }));
    expect(fetch.mock.calls[0][0]).toBe("/Prosumers/Decide/199012301234");
    expect(fetch.mock.calls[0][1].body.get("RequestId")).toBe("request-1");
    expect(fetch.mock.calls[0][1].body.get("Version")).toBe("5");
    expect(fetch.mock.calls[0][1].body.get("Decision")).toBe("Rejected");
  });

  it("updates energy availability for the selected window", async () => {
    const user = userEvent.setup();
    const slot = {
      id: "slot-1",
      startsAt: "2099-01-01T08:00:00Z",
      endsAt: "2099-01-01T09:00:00Z",
      usableSlots: 4,
      usableEnergyKwh: 40,
      availableSlots: 3,
      availableEnergyKwh: 30,
      activeReservations: 1,
      allocations: [],
    };
    const initial = {
      ...base,
      controller: "Stations",
      page: "Details",
      model: { ...station, slots: [slot] },
    };
    const fetch = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(response(initial));
    render(<App initialData={initial} />);
    await user.click(
      screen.getByRole("button", { name: "Update availability" }),
    );
    const inputs = screen.getAllByLabelText("Usable energy (kWh)");
    await user.clear(inputs[1]);
    await user.type(inputs[1], "35");
    await user.click(screen.getByRole("button", { name: "Save availability" }));
    expect(fetch.mock.calls[0][0]).toBe(
      "/Stations/Availability/station-1?slotId=slot-1",
    );
    expect(fetch.mock.calls[0][1].body.get("UsableEnergyKwh")).toBe("35");
    expect(fetch.mock.calls[0][1].body.get("Version")).toBe("7");
  });

  it("renders server access-denied responses through the React error page", () => {
    render(
      <App
        initialData={{
          ...base,
          controller: "Account",
          page: "Error",
          model: {
            statusCode: 403,
            message: "Your role does not have access to this page.",
          },
        }}
      />,
    );
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("403");
    expect(
      screen.getByText("Your role does not have access to this page."),
    ).toBeVisible();
  });

  it("preserves search filters in pagination links", () => {
    window.history.replaceState(
      null,
      "",
      "/Prosumers?pending=true&search=solar",
    );
    render(
      <App
        initialData={{
          ...base,
          controller: "Prosumers",
          page: "Index",
          model: { items: [], total: 30, page: 1, pageSize: 20 },
          meta: { Pending: true, Search: "solar" },
        }}
      />,
    );
    expect(screen.getByRole("link", { name: "Next →" })).toHaveAttribute(
      "href",
      "/Prosumers?pending=true&search=solar&page=2",
    );
  });
});
