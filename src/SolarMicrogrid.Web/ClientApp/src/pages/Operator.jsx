import { SolarScene } from "../SolarScene";
import {
  Form,
  Save,
  Stat,
  Table,
  dateTime,
  path,
  usePortal,
} from "../components";

export function Operator() {
  const { data } = usePortal();
  const meta = data.meta;
  return (
    <>
      <section className="overview-hero operator-hero">
        <div className="overview-copy">
          <p className="eyebrow">Grid operator workspace</p>
          <h1 className="display-title">Operator Dashboard</h1>
          <p className="hero-description">
            Monitor reservations. Keep energy moving.
          </p>
          <div className="mt-7 flex flex-wrap items-center gap-5">
            <a href="/Bookings/Pending" className="btn">
              Review pending bookings
            </a>
            <a href="/Bookings?status=COMPLETED" className="text-link">
              Completed operations →
            </a>
          </div>
        </div>
        <SolarScene />
      </section>
      <div className="metrics-grid metrics-grid-three">
        <Stat
          label="Pending"
          value={meta.PendingCount}
          note="Awaiting your approval"
        />
        <Stat
          label="Approved future"
          value={meta.ApprovedFutureCount}
          note="Ready for QR verification"
        />
        <Stat
          label="Completed"
          value={meta.CompletedCount}
          note="Energy transfers finalised"
        />
      </div>
      <OperationsTable bookings={meta.PendingReservations || []} pending />
      <OperationsTable bookings={meta.RecentCompleted || []} />
    </>
  );
}

function OperationsTable({ bookings, pending = false }) {
  return (
    <section className="card mb-6 overflow-hidden !p-0">
      <div className="section-toolbar">
        <h2>
          {pending
            ? "Recent pending reservations"
            : "Recent completed operations"}
        </h2>
        <a
          className="text-link"
          href={pending ? "/Bookings/Pending" : "/Bookings?status=COMPLETED"}
        >
          View all →
        </a>
      </div>
      <Table
        headings={[
          "Reservation ID",
          "Prosumer NIC",
          "Station",
          pending ? "Date & time (Sri Lanka)" : "Completed (Sri Lanka)",
          "Energy (kWh)",
          ...(pending ? ["Action"] : []),
        ]}
        empty={
          !bookings.length &&
          (pending ? "No pending bookings" : "No completed operations yet")
        }
      >
        {bookings.map((b) => (
          <tr key={b.id}>
            <td className="font-medium">{b.reservationId}</td>
            <td className="font-mono text-xs">{b.prosumerNic}</td>
            <td className="max-w-56 break-all text-xs">{b.stationId}</td>
            <td className="whitespace-nowrap">
              {dateTime(
                pending ? b.reservationDate : b.completedAt || b.updatedAt,
              )}
            </td>
            <td>{b.energyAmountKwh}</td>
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
  );
}
