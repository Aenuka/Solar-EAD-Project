import {
  Badge,
  Field,
  Form,
  Heading,
  Save,
  Stat,
  Table,
  dateTime,
  path,
  usePortal,
} from "../components";
import { BookingStation, BookingProsumer } from "../bookingPresentation";

export function Bookings() {
  const { data } = usePortal();
  const dashboard = data.page === "Dashboard";
  const pending = data.page === "Pending";
  const bookings = dashboard
    ? data.meta.RecentBookings || []
    : data.model || [];
  const stats = dashboard
    ? [
        data.meta.TotalCount,
        data.meta.PendingCount,
        data.meta.ApprovedFutureCount,
        data.meta.CompletedCount,
      ]
    : [
        bookings.length,
        ...["PENDING", "APPROVED", "COMPLETED"].map(
          (s) => bookings.filter((b) => b.status === s).length,
        ),
      ];
  return (
    <>
      <Heading
        eyebrow="Energy trading"
        title={
          dashboard
            ? "Booking dashboard"
            : pending
              ? "Pending reservations"
              : "Booking monitoring"
        }
        description={
          pending
            ? "Review reservations awaiting operator approval."
            : "A clear view of energy reservations across your microgrid."
        }
      >
        <div className="flex flex-wrap gap-2">
          {data.page !== "Index" && (
            <a href="/Bookings" className="btn btn-secondary">
              All bookings
            </a>
          )}
          {!dashboard && (
            <a href="/Bookings/Dashboard" className="btn btn-secondary">
              Dashboard
            </a>
          )}
          {!pending && (
            <a href="/Bookings/Pending" className="btn">
              Review pending
            </a>
          )}
        </div>
      </Heading>
      {!pending && (
        <div className="metrics-grid">
          {[
            "Total bookings",
            "Pending",
            dashboard ? "Upcoming approved" : "Approved",
            "Completed",
          ].map((label, i) => (
            <Stat
              key={label}
              label={label}
              value={stats[i]}
              note={
                [
                  "All reservations",
                  "Awaiting approval",
                  "Ready for QR verification",
                  "Energy transfer finalised",
                ][i]
              }
            />
          ))}
        </div>
      )}
      <section className="card overflow-hidden !p-0">
        {!dashboard && !pending && (
          <form method="get" action="/Bookings" className="filter-toolbar">
            <Field
              label="Reservation status"
              name="status"
              value={data.meta.FilterStatus}
              options={[
                ["", "All statuses"],
                "PENDING",
                "APPROVED",
                "CANCELLED",
                "COMPLETED",
              ]}
            />
            <div className="min-w-48 flex-1">
              <Field
                label="Station"
                name="stationId"
                value={data.meta.FilterStation}
                options={[
                  ["", "All stations"],
                  ...(data.meta.Stations || []).map((s) => [
                    s.id,
                    `${s.name} · ${s.address}${s.active ? "" : " (inactive)"}`,
                  ]),
                  ...(data.meta.FilterStation &&
                  !(data.meta.Stations || []).some(
                    (s) => s.id === data.meta.FilterStation,
                  )
                    ? [[data.meta.FilterStation, "Station unavailable"]]
                    : []),
                ]}
              />
            </div>
            <button type="submit" className="btn btn-secondary">
              Search
            </button>
            <a href="/Bookings" className="text-link pb-3">
              Clear
            </a>
          </form>
        )}
        {(dashboard || pending) && (
          <div className="p-6">
            <h2>{dashboard ? "Recent activity" : "Action required"}</h2>
            <p className="muted mt-2">
              {dashboard
                ? "Your five most recent reservations."
                : "Approved bookings become eligible for QR verification."}
            </p>
          </div>
        )}
        <Table
          headings={[
            "Station & energy window (Sri Lanka)",
            "Prosumer",
            "Booked for (Sri Lanka)",
            "Energy (kWh)",
            "Status",
            ...(pending ? ["Action"] : []),
          ]}
          empty={
            !bookings.length &&
            (pending
              ? "All caught up. No pending reservations."
              : "No bookings found")
          }
        >
          {bookings.map((b) => (
            <tr key={b.id}>
              <td>
                <BookingStation booking={b} />
              </td>
              <td>
                <BookingProsumer booking={b} />
              </td>
              <td className="whitespace-nowrap">
                {dateTime(b.reservationDate)}
              </td>
              <td>{b.energyAmountKwh}</td>
              <td>
                <Badge>{b.status}</Badge>
              </td>
              {pending && (
                <td>
                  <Form action={path("Bookings", "Approve", b.id)}>
                    <Save>Approve</Save>
                  </Form>
                </td>
              )}
            </tr>
          ))}
        </Table>
      </section>
    </>
  );
}
