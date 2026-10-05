package lk.solar.microgrid.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

public final class BookingPresentationTest {
    @Test public void namesAreShownWhileServerIdentityIsPreserved() throws Exception {
        Reservation booking = new Reservation(new JSONObject().put("stationId", "opaque-id")
                .put("stationName", "Coastal Solar").put("prosumerName", "Nimal Perera"));
        assertEquals("opaque-id", booking.stationId);
        assertEquals("Coastal Solar", booking.stationLabel());
        assertEquals("Nimal Perera", booking.prosumerLabel());
        assertFalse(booking.summary().contains("opaque-id"));
    }
    @Test public void legacyOrMissingRelationsNeverBecomeOpaqueDisplayLabels() throws Exception {
        Reservation booking = new Reservation(new JSONObject().put("stationId", "opaque-id")
                .put("stationName", JSONObject.NULL).put("slotId", "opaque-window"));
        assertEquals("Station unavailable", booking.stationLabel());
        assertEquals("Energy window unavailable", booking.windowLabel());
    }
    @Test public void windowTimesUseSriLankaEvenWhenTheDeviceUsesAnotherZone() {
        Locale previousLocale = Locale.getDefault(); TimeZone previousZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.UK); TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            assertEquals("Thu, 1 Oct 2026 · 1:30 pm–2:30 pm", BookingText.window("2026-10-01T08:00:00Z", "2026-10-01T09:00:00Z"));
            assertTrue(BookingText.date("2026-10-01T08:00:00Z").contains("1:30 pm"));
        } finally { Locale.setDefault(previousLocale); TimeZone.setDefault(previousZone); }
    }
    private JSONObject slot(String id, String start, int slots, double energy) throws Exception {
        return new JSONObject().put("id", id).put("startsAt", start)
                .put("endsAt", Instant.parse(start).plusSeconds(3600).toString())
                .put("availableSlots", slots).put("availableEnergyKwh", energy);
    }
    @Test public void editingKeepsTheCurrentWindowSelectableWhenItsInventoryIsFullyReserved() throws Exception {
        var slots = new JSONArray().put(slot("current", "2026-10-07T08:00:00Z", 0, 0))
                .put(slot("other-full", "2026-10-08T08:00:00Z", 0, 0));
        var options = BookingWindow.available(slots, "current", 10, Instant.parse("2026-10-05T08:00:00Z"));
        assertEquals(1, options.size()); assertEquals("current", options.get(0).id);
        assertEquals(10, options.get(0).availableEnergy, 0.001);
        assertEquals(1, options.get(0).availableSlots);
    }
    @Test public void newBookingsExcludeFullPastAndOutOfRangeWindows() throws Exception {
        var slots = new JSONArray().put(slot("past", "2026-10-04T08:00:00Z", 1, 10))
                .put(slot("too-late", "2026-10-13T08:00:00Z", 1, 10))
                .put(slot("full", "2026-10-08T08:00:00Z", 0, 10))
                .put(slot("valid", "2026-10-07T08:00:00Z", 1, 10));
        var options = BookingWindow.available(slots, null, 0, Instant.parse("2026-10-05T08:00:00Z"));
        assertEquals(1, options.size()); assertEquals("valid", options.get(0).id);
    }
}
