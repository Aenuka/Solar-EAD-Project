package lk.solar.microgrid.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class StationTest {
    private JSONObject station() throws Exception {
        return new JSONObject("{\"id\":\"stable-station-id\",\"name\":\"Colombo Solar\",\"address\":\"10 Solar Road\",\"latitude\":6.9271,\"longitude\":79.8612,\"capacityKw\":50,\"storageKwh\":100,\"batterySlots\":4,\"schedule\":{\"days\":[1,2],\"opensAt\":\"08:00\",\"closesAt\":\"18:00\"},\"slots\":[{\"startsAt\":\"2026-10-01T03:30:00Z\",\"endsAt\":\"2026-10-01T04:30:00Z\",\"availableSlots\":2,\"availableEnergyKwh\":30}]}");
    }
    @Test public void preservesServerIdentityAndCoordinates() throws Exception {
        Station station = new Station(station());
        assertEquals("stable-station-id", station.id);
        assertEquals(6.9271, station.latitude, 0.000001);
        assertEquals(79.8612, station.longitude, 0.000001);
        assertTrue(station.details().contains("2 slots, 30.0 kWh available"));
        assertTrue(station.details().contains("09:00"));
    }
    @Test(expected = JSONException.class) public void rejectsImpossibleCoordinates() throws Exception {
        new Station(station().put("latitude", 91));
    }
    @Test(expected = JSONException.class) public void requiresStableIdentity() throws Exception {
        JSONObject json = station(); json.remove("id"); new Station(json);
    }
    @Test public void explainsEmptyAvailability() throws Exception {
        Station station = new Station(station().put("slots", new org.json.JSONArray()));
        assertTrue(station.details().contains("No upcoming energy windows"));
    }
}
