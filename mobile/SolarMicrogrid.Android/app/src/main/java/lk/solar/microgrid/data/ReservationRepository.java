package lk.solar.microgrid.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservation data access — calls the central API.
 * Author: Sajith
 */
public final class ReservationRepository {
    private final ApiClient api;

    public ReservationRepository(ApiClient api) {
        this.api = api;
    }

    /** POST /reservations */
    public Reservation create(String nic, String stationId, String slotId,
                              String reservationDate, double energyAmountKwh,
                              String tradingType, String token)
            throws IOException, JSONException, ApiException {
        JSONObject body = new JSONObject()
            .put("prosumerNic", nic.trim())
            .put("stationId", stationId.trim())
            .put("slotId", slotId.trim())
            .put("reservationDate", reservationDate)
            .put("energyAmountKwh", energyAmountKwh)
            .put("tradingType", tradingType.trim());
        JSONObject response = api.request("POST", "reservations", body, token);
        return new Reservation(response);
    }

    /** GET /reservations/history?nic=... */
    public List<Reservation> history(String nic, String token)
            throws IOException, JSONException, ApiException {
        return parseList(api.requestArray("GET",
            "reservations/history?nic=" + nic, null, token));
    }

    /** GET /reservations/pending */
    public List<Reservation> pending(String token)
            throws IOException, JSONException, ApiException {
        return parseList(api.requestArray("GET",
            "reservations/pending", null, token));
    }

    /** GET /reservations/search */
    public List<Reservation> search(String status, String stationId, String token)
            throws IOException, JSONException, ApiException {
        StringBuilder path = new StringBuilder("reservations/search?");
        if (status != null && !status.isEmpty()) path.append("status=").append(status).append("&");
        if (stationId != null && !stationId.isEmpty()) path.append("stationId=").append(stationId);
        return parseList(api.requestArray("GET", path.toString(), null, token));
    }

    /** PATCH /reservations/{id}/cancel */
    public Reservation cancel(String id, String reason, String token)
            throws IOException, JSONException, ApiException {
        JSONObject body = new JSONObject().put("reason", reason == null ? "" : reason);
        JSONObject response = api.request("PATCH", "reservations/" + id + "/cancel", body, token);
        return new Reservation(response);
    }

    /** PUT /reservations/{id} */
    public Reservation update(String id, String slotId, String reservationDate,
                              double energyAmountKwh, String tradingType, String token)
            throws IOException, JSONException, ApiException {
        JSONObject body = new JSONObject()
            .put("slotId", slotId.trim())
            .put("reservationDate", reservationDate)
            .put("energyAmountKwh", energyAmountKwh)
            .put("tradingType", tradingType.trim());
        JSONObject response = api.request("PUT", "reservations/" + id, body, token);
        return new Reservation(response);
    }

    private static List<Reservation> parseList(JSONArray array) throws JSONException {
        List<Reservation> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(new Reservation(array.getJSONObject(i)));
        }
        return list;
    }
}