package lk.solar.microgrid.data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Booking times are always shown in the station's Sri Lanka time zone. */
public final class BookingText {
    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
    private BookingText() { }

    public static String date(String value) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(COLOMBO)
                    .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.getDefault()));
        } catch (DateTimeParseException | NullPointerException e) { return "Time unavailable"; }
    }

    public static String window(String startsAt, String endsAt) {
        try {
            var start = OffsetDateTime.parse(startsAt).atZoneSameInstant(COLOMBO);
            var end = OffsetDateTime.parse(endsAt).atZoneSameInstant(COLOMBO);
            var time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
            return start.toLocalDate().equals(end.toLocalDate())
                    ? start.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
                        + " · " + start.format(time) + "–" + end.format(time)
                    : date(startsAt) + " – " + date(endsAt);
        } catch (DateTimeParseException | NullPointerException e) { return "Energy window unavailable"; }
    }

    public static String trading(String value) {
        if (value == null || value.isEmpty()) return "Energy transfer";
        if ("DROP_OFF".equals(value)) return "Drop off";
        if ("PICK_UP".equals(value)) return "Pick up";
        return value.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
