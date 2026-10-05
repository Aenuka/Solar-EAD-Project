package lk.solar.microgrid.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reservation model — mirrors SolarMicrogrid.Contracts.ReservationResponse.
 * Author: Sajith & Lakshman K A P C
 */
public final class Reservation {
    public final String id, reservationId, prosumerNic, stationId, slotId;
    public final String stationName, stationAddress, prosumerName, slotStartsAt, slotEndsAt;
    public final String reservationDate, tradingType, status, createdAt, updatedAt, transactionToken;
    public final double energyAmountKwh;
    public final int allocationSlots;
    public final JSONObject source;

    public Reservation(JSONObject json) throws JSONException {
        // Initialize reservation from JSON response
        source = json;
        id = json.optString("id", "");
        reservationId = json.optString("reservationId", "");
        prosumerNic = json.optString("prosumerNic", "");
        prosumerName = json.optString("prosumerName", "");
        stationId = json.optString("stationId", "");
        stationName = json.optString("stationName", "");
        slotId = json.optString("slotId", "");
        stationName = json.optString("stationName", "");
        stationAddress = json.optString("stationAddress", "");
        prosumerName = json.optString("prosumerName", "");
        slotStartsAt = json.optString("slotStartsAt", "");
        slotEndsAt = json.optString("slotEndsAt", "");
        reservationDate = json.optString("reservationDate", "");
        energyAmountKwh = json.optDouble("energyAmountKwh", 0);
        tradingType = json.optString("tradingType", "");
        status = json.optString("status", "");
        createdAt = json.optString("createdAt", "");
        updatedAt = json.optString("updatedAt", "");
        transactionToken = json.optString("transactionToken", "");
        allocationSlots = json.optInt("allocationSlots", 0);
    }

    public String summary() {
        // Generate a summary string for the reservation
        return String.format(java.util.Locale.getDefault(),
            "%s · %.2f kWh · %s", stationLabel(), energyAmountKwh, status);
    }

    public String stationLabel() { 
        // Get the station label or a fallback if unavailable
        return stationName.isBlank() ? "Station unavailable" : stationName; 
    }
    public String prosumerLabel() { 
        // Get the prosumer name or a fallback if unavailable
        return prosumerName.isBlank() ? "Name unavailable" : prosumerName; 
    }
    public String windowLabel() { 
        // Get the formatted time window label
        return BookingText.window(slotStartsAt, slotEndsAt); 
    }
}
