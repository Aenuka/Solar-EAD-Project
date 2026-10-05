package lk.solar.microgrid.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Server-owned inventory. List responses include the next window; detail returns all future windows. */
public final class Station {
    public final String id, name, address;
    public final double latitude, longitude, capacityKw, storageKwh;
    public final int batterySlots;
    public final JSONObject source;
    // Parses station data and rejects coordinates outside valid GPS ranges. *****
    public Station(JSONObject json) throws JSONException {
        source = json;
        id = json.getString("id"); name = json.getString("name"); address = json.getString("address");
        latitude = json.getDouble("latitude"); longitude = json.getDouble("longitude");
        capacityKw = json.getDouble("capacityKw"); storageKwh = json.getDouble("storageKwh");
        batterySlots = json.getInt("batterySlots");
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new JSONException("Invalid station coordinates");
    }
    // Summarizes the station's power, storage, and battery slot capacity. *****
    public String summary() {
        return String.format(Locale.getDefault(), "%.2f kW · %.2f kWh · %d battery slots", capacityKw, storageKwh, batterySlots);
    }
    // Formats operating hours and future availability in Sri Lanka time. *****
    public String details() throws JSONException {
        JSONObject schedule = source.getJSONObject("schedule");
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        JSONArray days = schedule.getJSONArray("days");
        StringBuilder text = new StringBuilder(name).append('\n').append(address).append('\n').append(summary())
            .append("\nGPS: ").append(latitude).append(", ").append(longitude).append("\nOpen: ");
        for (int i = 0; i < days.length(); i++) {
            int day = days.getInt(i);
            if (day < 0 || day > 6) throw new JSONException("Invalid operating day");
            text.append(names[day]).append(' ');
        }
        text.append(schedule.getString("opensAt")).append("–").append(schedule.getString("closesAt"))
            .append(" (Sri Lanka)\nStation ID: ").append(id).append("\n\nEnergy windows:\n");
        JSONArray slots = source.getJSONArray("slots");
        if (slots.length() == 0) text.append("No upcoming energy windows.\n");
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM HH:mm");
        for (int i = 0; i < slots.length(); i++) {
            JSONObject slot = slots.getJSONObject(i);
            text.append(OffsetDateTime.parse(slot.getString("startsAt")).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30)).format(format))
                .append(" – ").append(OffsetDateTime.parse(slot.getString("endsAt")).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30)).format(format))
                .append("\n").append(slot.getInt("availableSlots")).append(" slots, ")
                .append(slot.getDouble("availableEnergyKwh")).append(" kWh available\n\n");
        }
        return text.append("Availability is checked again when a booking is created.").toString();
    }
}
