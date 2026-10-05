package lk.solar.microgrid.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** A selectable energy window, with credit for inventory already held by this booking. */
public final class BookingWindow {
    public final String id, startsAt, endsAt;
    public final int availableSlots;
    public final double availableEnergy;
    private BookingWindow(JSONObject json, String currentSlotId, double bookedEnergy) throws JSONException {
        id = json.getString("id"); startsAt = json.getString("startsAt"); endsAt = json.getString("endsAt");
        boolean current = id.equals(currentSlotId);
        availableSlots = json.getInt("availableSlots") + (current ? 1 : 0);
        availableEnergy = json.getDouble("availableEnergyKwh") + (current ? bookedEnergy : 0);
    }
    public String label() {
        return BookingText.window(startsAt, endsAt) + "\n" + availableEnergy + " kWh · " + availableSlots + " battery slots available";
    }
    public static List<BookingWindow> available(JSONArray slots, String currentSlotId, double bookedEnergy, Instant now) throws JSONException {
        List<BookingWindow> result = new ArrayList<>();
        for (int i = 0; i < slots.length(); i++) {
            BookingWindow window = new BookingWindow(slots.getJSONObject(i), currentSlotId, bookedEnergy);
            Instant start = OffsetDateTime.parse(window.startsAt).toInstant();
            if (start.isAfter(now) && !start.isAfter(now.plusSeconds(7 * 86400L)) &&
                    window.availableSlots > 0 && Double.isFinite(window.availableEnergy) && window.availableEnergy > 0)
                result.add(window);
        }
        return result;
    }
}
