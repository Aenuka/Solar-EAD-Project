import { Battery, MapPin, Sun, Zap } from "lucide-react";
import {
  Badge,
  Empty,
  Expand,
  Field,
  Form,
  Heading,
  Hidden,
  Modal,
  Pagination,
  Save,
  Stat,
  Table,
  dateTime,
  path,
  usePortal,
} from "../components";

// Routes staff to station listing, registration, editing, or details. *****
export function Stations() {
  const { data } = usePortal();
  const m = data.model;
  const backoffice = data.user.role === "Backoffice";
  if (data.page === "Details")
    return <StationDetails station={m} backoffice={backoffice} />;
  if (["Create", "Edit"].includes(data.page))
    return <StationForm model={m} create={data.page === "Create"} />;
  return (
    <>
      <Heading
        eyebrow="Clean energy, closer"
        title="Microgrid stations"
        description="Manage locations, operating schedules, and available energy."
      >
        {backoffice && (
          <a className="btn" href="/Stations/Create">
            Register station
          </a>
        )}
      </Heading>
      {!m.items.length ? (
        <section className="card">
          <Empty title="No stations registered yet">
            Your community’s energy network starts here.
          </Empty>
        </section>
      ) : (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {m.items.map((s) => (
            <a
              key={s.id}
              href={path("Stations", "Details", s.id)}
              className="card station-card group"
            >
              <div className="station-visual" aria-hidden="true">
                <svg viewBox="0 0 160 100" width="140" fill="none">
                  <path
                    d="M53 64v23M109 48v38"
                    stroke="#bbc2cc"
                    strokeWidth="5"
                  />
                  <path
                    d="m19 41 76-26 49 43-76 27Z"
                    fill="#163d6b"
                    stroke="#b5c7dc"
                    strokeWidth="3"
                  />
                  <path
                    d="m35 36 49 43M52 30l49 43M68 25l49 43M85 20l49 43M32 52l76-26M45 63l76-26M57 74l76-26"
                    stroke="#82a8d0"
                    strokeWidth="0.8"
                  />
                  <circle cx="128" cy="17" r="11" fill="#f7d799" />
                </svg>
              </div>
              <div className="mb-4 flex justify-between">
                <span className="eyebrow mb-0 self-center">
                  Microgrid station
                </span>
                <Badge>{s.active ? "Active" : "Inactive"}</Badge>
              </div>
              <h2 className="group-hover:text-sky-700">{s.name}</h2>
              <p className="muted mt-2 flex items-start gap-1.5">
                <MapPin className="mt-0.5 shrink-0" size={15} />
                {s.address}
              </p>
              <div className="my-6 grid grid-cols-3 gap-2 border-y border-slate-100 py-5">
                <div>
                  <strong className="text-lg">{s.capacityKw}</strong>
                  <p className="muted text-xs">kW power</p>
                </div>
                <div>
                  <strong className="text-lg">{s.storageKwh}</strong>
                  <p className="muted text-xs">kWh storage</p>
                </div>
                <div>
                  <strong className="text-lg">{s.batterySlots}</strong>
                  <p className="muted text-xs">battery slots</p>
                </div>
              </div>
              <p className="text-sm">
                {s.activeReservations} active reservations
              </p>
              <p className="muted mt-2 text-xs">
                {s.schedule.opensAt}–{s.schedule.closesAt} ·{" "}
                {s.schedule.timeZone}
              </p>
              <span className="text-link mt-5">Manage station →</span>
            </a>
          ))}
        </div>
      )}
      <Pagination model={m} />
    </>
  );
}

// Collects a station's location and physical capacity for creation or editing. *****
function StationForm({ model: m, create }) {
  // The edit DTO has no ID; it stays in the existing route.
  const id =
    new URL(window.location.href).pathname.split("/")[3] ||
    new URLSearchParams(window.location.search).get("id");
  return (
    <>
      <a
        className="text-link mb-6"
        href={create ? "/Stations" : path("Stations", "Details", id)}
      >
        ← Back to stations
      </a>
      <Heading
        title={create ? "A new point of connection." : "Edit station"}
        description="Set up the location and capacity of your microgrid station."
      />
      <section className="card max-w-3xl">
        <Form
          action={create ? "/Stations/Create" : path("Stations", "Edit", id)}
          version={create ? null : m.version}
        >
          <Field
            name="Name"
            label="Station name"
            value={m.name}
            minLength={2}
            maxLength={100}
            required
          />
          <Field
            name="Address"
            label="Address"
            value={m.address}
            minLength={5}
            maxLength={300}
            required
          />
          <div className="form-grid">
            <Field
              name="Latitude"
              label="Latitude"
              type="number"
              value={m.latitude}
              min={-90}
              max={90}
              step="any"
              required
            />
            <Field
              name="Longitude"
              label="Longitude"
              type="number"
              value={m.longitude}
              min={-180}
              max={180}
              step="any"
              required
            />
            <Field
              name="CapacityKw"
              label="Power capacity (kW)"
              type="number"
              value={m.capacityKw || ""}
              min={0.01}
              max={1000000}
              step="any"
              required
            />
            <Field
              name="StorageKwh"
              label="Energy storage (kWh)"
              type="number"
              value={m.storageKwh || ""}
              min={0.01}
              max={1000000}
              step="any"
              required
            />
            <Field
              name="BatterySlots"
              label="Battery slots"
              type="number"
              value={m.batterySlots || ""}
              min={1}
              max={10000}
              step={1}
              required
            />
          </div>
          <div className="flex items-center gap-4">
            <Save>{create ? "Register station" : "Save station"}</Save>
            <a className="text-link" href="/Stations">
              Cancel
            </a>
          </div>
        </Form>
      </section>
    </>
  );
}

// Keeps window availability fields within the station's physical limits. *****
function CapacityFields({ station, slot }) {
  return (
    <div className="form-grid">
      <Field
        name="UsableSlots"
        label="Usable battery slots"
        type="number"
        min={0}
        max={station.batterySlots}
        step={1}
        value={slot?.usableSlots ?? station.batterySlots}
        required
      />
      <Field
        name="UsableEnergyKwh"
        label="Usable energy (kWh)"
        type="number"
        min={0}
        max={station.storageKwh}
        step="0.01"
        value={slot?.usableEnergyKwh}
        required
      />
    </div>
  );
}

// Shows station status, schedule, energy windows, and staff actions. *****
function StationDetails({ station: s, backoffice }) {
  const action = (name, slotId) =>
    path("Stations", name, s.id) +
    (slotId ? `?slotId=${encodeURIComponent(slotId)}` : "");
  return (
    <>
      <a href="/Stations" className="text-link mb-6">
        ← Microgrid stations
      </a>
      <Heading
        eyebrow="Station workspace"
        title={s.name}
        description={s.address}
      >
        {backoffice && (
          <a
            className="btn btn-secondary"
            href={path("Stations", "Edit", s.id)}
          >
            Edit station
          </a>
        )}
      </Heading>
      <div className="metrics-grid">
        <Stat
          label="Power capacity"
          value={`${s.capacityKw} kW`}
          note="Station generation capacity"
          icon={Zap}
        />
        <Stat
          label="Energy storage"
          value={`${s.storageKwh} kWh`}
          note="Total storage capacity"
          icon={Battery}
        />
        <Stat
          label="Battery slots"
          value={s.batterySlots}
          note="Station battery capacity"
          icon={Battery}
        />
        <Stat
          label="Active reservations"
          value={s.activeReservations}
          note="Across all energy windows"
          icon={Sun}
        />
      </div>
      <div className="mb-6 grid items-start gap-6 lg:grid-cols-3">
        <section className="card">
          <div className="mb-5 flex items-center justify-between">
            <h2>Station status</h2>
            <Badge>{s.active ? "Active" : "Inactive"}</Badge>
          </div>
          <p className="muted mb-5">
            GPS: {s.latitude}, {s.longitude}
          </p>
          {backoffice && (
            <Form
              action={action("Status")}
              version={s.version}
              confirm={
                s.active
                  ? "Deactivate this station? It will stop accepting new reservations. Stations with active reservations cannot be deactivated."
                  : "Activate this station for new energy windows and reservations?"
              }
            >
              <Hidden name="Active" value={String(!s.active)} />
              <button
                type="submit"
                className={`btn ${s.active ? "btn-danger" : ""}`}
              >
                {s.active ? "Deactivate station" : "Activate station"}
              </button>
            </Form>
          )}
        </section>
        <section className="card lg:col-span-2">
          <h2>Operating schedule</h2>
          <p className="muted mb-5 mt-2">
            Asia/Colombo (UTC+05:30). Overnight windows are not supported.
          </p>
          <Form action={action("Schedule")} version={s.version}>
            <fieldset>
              <legend className="mb-3 text-sm font-medium">
                Operating days
              </legend>
              <div className="flex flex-wrap gap-2">
                {[
                  "Sunday",
                  "Monday",
                  "Tuesday",
                  "Wednesday",
                  "Thursday",
                  "Friday",
                  "Saturday",
                ].map((day, index) => (
                  <label key={day} className="schedule-day">
                    <input
                      type="checkbox"
                      name="Days"
                      value={index}
                      defaultChecked={s.schedule.days.includes(index)}
                    />
                    {day.slice(0, 3)}
                  </label>
                ))}
              </div>
            </fieldset>
            <div className="form-grid">
              <Field
                name="OpensAt"
                label="Opens"
                type="time"
                value={s.schedule.opensAt}
                required
              />
              <Field
                name="ClosesAt"
                label="Closes"
                type="time"
                value={s.schedule.closesAt}
                required
              />
            </div>
            <Save>Save schedule</Save>
          </Form>
        </section>
      </div>
      <div className="mb-5 mt-9 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2>Energy windows & usage</h2>
          <p className="muted mt-2">All times are shown in Sri Lanka time.</p>
        </div>
        <Modal title="Add energy window" trigger="Add energy window">
          <p className="muted mb-5">
            Windows cannot overlap. Energy must fit both storage and power ×
            duration.
          </p>
          <Form action={action("AddSlot")} version={s.version}>
            <Field
              name="StartsAt"
              label="Starts at (Sri Lanka time)"
              type="datetime-local"
              required
            />
            <Field
              name="EndsAt"
              label="Ends at (Sri Lanka time)"
              type="datetime-local"
              required
            />
            <CapacityFields station={s} />
            <Save>Add window</Save>
          </Form>
        </Modal>
      </div>
      {!s.slots.length && (
        <section className="card">
          <Empty title="Ready for your first energy window">
            Add a time window to make energy available for reservations.
          </Empty>
        </section>
      )}
      <div className="space-y-5">
        {s.slots.map((slot) => (
          <section key={slot.id} className="card">
            <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
              <div>
                <h3>
                  {dateTime(slot.startsAt)} — {dateTime(slot.endsAt)}
                </h3>
                <p className="muted mt-2">
                  {slot.activeReservations} active reservations ·{" "}
                  {slot.usableSlots} usable slots · {slot.usableEnergyKwh} kWh
                  usable energy
                </p>
              </div>
              <span className="rounded-full bg-emerald-50 px-4 py-2 text-sm text-emerald-800">
                {slot.availableEnergyKwh} kWh / {slot.availableSlots} slots
                available
              </span>
            </div>
            {new Date(slot.endsAt) > new Date() && (
              <Expand label="Update availability">
                <p className="muted mb-4">
                  Set total capacity, including slots and energy already
                  reserved or used.
                </p>
                <Form
                  action={action("Availability", slot.id)}
                  version={s.version}
                >
                  <CapacityFields station={s} slot={slot} />
                  <Save>Save availability</Save>
                </Form>
              </Expand>
            )}
            {slot.allocations.length > 0 && (
              <div className="mt-5">
                <Table
                  headings={[
                    "Booking allocation ID",
                    "Slots",
                    "Energy (kWh)",
                    "Status",
                  ]}
                >
                  {slot.allocations.map((a) => (
                    <tr key={a.bookingId}>
                      <td>{a.bookingId}</td>
                      <td>{a.slots}</td>
                      <td>{a.energyKwh}</td>
                      <td>
                        <Badge>{a.status}</Badge>
                      </td>
                    </tr>
                  ))}
                </Table>
              </div>
            )}
            <div className="mt-5 border-t border-slate-100 pt-5">
              <Form
                action={action("Archive", slot.id)}
                version={s.version}
                confirm="Remove this energy window? Windows with active reservations cannot be removed."
              >
                <button className="text-link !text-red-700" type="submit">
                  {new Date(slot.endsAt) <= new Date()
                    ? "Archive window"
                    : "Remove unused window"}
                </button>
              </Form>
            </div>
          </section>
        ))}
      </div>
    </>
  );
}
