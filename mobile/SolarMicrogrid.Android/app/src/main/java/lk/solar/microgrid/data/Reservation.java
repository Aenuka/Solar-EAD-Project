package lk.solar.microgrid.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reservation model — mirrors SolarMicrogrid.Contracts.ReservationResponse.
 * Author: Sajith
 */
public final class Reservation {
    public final String id, reservationId, prosumerNic, stationId, slotId;
    public final String reservationDate, tradingType, status, createdAt, updatedAt, transactionToken;
    public final double energyAmountKwh;
    public final JSONObject source;

    public Reservation(JSONObject json) throws JSONException {
        source = json;
        id = json.optString("id", "");
        reservationId = json.optString("reservationId", "");
        prosumerNic = json.optString("prosumerNic", "");
        stationId = json.optString("stationId", "");
        slotId = json.optString("slotId", "");
        reservationDate = json.optString("reservationDate", "");
        energyAmountKwh = json.optDouble("energyAmountKwh", 0);
        tradingType = json.optString("tradingType", "");
        status = json.optString("status", "");
        createdAt = json.optString("createdAt", "");
        updatedAt = json.optString("updatedAt", "");
        transactionToken = json.optString("transactionToken", "");
    }

    public String summary() {
        return String.format(java.util.Locale.getDefault(),
            "%s · %.2f kWh · %s", stationId, energyAmountKwh, status);
    }
}