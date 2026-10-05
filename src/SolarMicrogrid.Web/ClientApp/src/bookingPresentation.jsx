import { dateTime, path } from "./components";

export function bookingWindow(booking) {
  if (!booking.slotStartsAt || !booking.slotEndsAt)
    return "Energy window unavailable";
  const start = new Date(booking.slotStartsAt);
  const end = new Date(booking.slotEndsAt);
  if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime()))
    return "Energy window unavailable";
  const day = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Colombo",
    day: "numeric",
    month: "short",
    year: "numeric",
  });
  const time = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Colombo",
    hour: "2-digit",
    minute: "2-digit",
  });
  return day.format(start) === day.format(end)
    ? `${day.format(start)} · ${time.format(start)}–${time.format(end)}`
    : `${dateTime(booking.slotStartsAt)} – ${dateTime(booking.slotEndsAt)}`;
}

export function BookingStation({ booking }) {
  return (
    <div className="min-w-52 max-w-80">
      {booking.stationName ? (
        <a
          className="text-link font-medium"
          href={path("Stations", "Details", booking.stationId)}
        >
          {booking.stationName}
        </a>
      ) : (
        <span className="font-medium">Station unavailable</span>
      )}
      {booking.stationAddress && (
        <span className="muted mt-1 block text-xs">
          {booking.stationAddress}
        </span>
      )}
      <span className="mt-2 block text-sm">{bookingWindow(booking)}</span>
      {booking.reservationId && (
        <span className="muted mt-1 block text-xs">
          Reference · {booking.reservationId}
        </span>
      )}
    </div>
  );
}

export function BookingProsumer({ booking }) {
  return (
    <div>
      <span className="font-medium">
        {booking.prosumerName || "Name unavailable"}
      </span>
      <span className="muted mt-1 block text-xs">
        NIC · {booking.prosumerNic}
      </span>
    </div>
  );
}
