import {
  Form,
  Heading,
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
      <Heading
        eyebrow="Grid operator workspace"
        title="Operator Dashboard"
        description="Monitor pending reservations and recent completed energy transfers."
      >
        <a href="/Bookings/Pending" className="btn">
          Review pending bookings
        </a>
      </Heading>
      <div className="mb-7 grid gap-4 sm:grid-cols-3">
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
      <div className="flex flex-wrap items-center justify-between gap-4 p-6">
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
