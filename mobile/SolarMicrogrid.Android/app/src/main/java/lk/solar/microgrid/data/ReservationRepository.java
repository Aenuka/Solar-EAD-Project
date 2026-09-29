package lk.solar.microgrid.data;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reservation data access — calls the central API on a background thread.
 * Author: Sajith
 */
public final class ReservationRepository {
    private final ApiClient api;
    private final AccountRepository accounts;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback<T> { void success(T result); void failure(int status, String message); }
    private interface Work<T> { T run() throws Exception; }

    public ReservationRepository(ApiClient api, AccountRepository accounts) {
        this.api = api;
        this.accounts = accounts;
    }

    // ===== CREATE =====
    public void create(String stationId, String slotId, String reservationDate,
                       double energyAmountKwh, String tradingType,
                       Callback<Reservation> callback) {
        run(() -> {
            JSONObject body = new JSONObject()
                .put("prosumerNic", requireNic().trim())
                .put("stationId", stationId.trim())
                .put("slotId", slotId.trim())
                .put("reservationDate", reservationDate)
                .put("energyAmountKwh", energyAmountKwh)
                .put("tradingType", tradingType.trim());
            JSONObject response = api.request("POST", "reservations", body, requireToken());
            return new Reservation(response);
        }, callback);
    }

    // ===== UPDATE =====
    public void update(String id, String slotId, String reservationDate,
                       double energyAmountKwh, String tradingType,
                       Callback<Reservation> callback) {
        run(() -> {
            JSONObject body = new JSONObject()
                .put("slotId", slotId.trim())
                .put("reservationDate", reservationDate)
                .put("energyAmountKwh", energyAmountKwh)
                .put("tradingType", tradingType.trim());
            JSONObject response = api.request("PUT", "reservations/" + id, body, requireToken());
            return new Reservation(response);
        }, callback);
    }

    // ===== CANCEL =====
    public void cancel(String id, String reason, Callback<Reservation> callback) {
        run(() -> {
            JSONObject body = new JSONObject().put("reason", reason == null ? "" : reason);
            JSONObject response = api.request("PATCH", "reservations/" + id + "/cancel",
                                              body, requireToken());
            return new Reservation(response);
        }, callback);
    }

    // ===== HISTORY =====
    public void history(Callback<List<Reservation>> callback) {
        run(() -> parseList(api.requestArray("GET",
            "reservations/history?nic=" + requireNic(), null, requireToken())), callback);
    }

    // ===== PENDING =====
    public void pending(Callback<List<Reservation>> callback) {
        run(() -> parseList(api.requestArray("GET",
            "reservations/pending", null, requireToken())), callback);
    }

    // ===== SEARCH =====
    public void search(String status, String stationId, Callback<List<Reservation>> callback) {
        run(() -> {
            StringBuilder path = new StringBuilder("reservations/search?");
            if (status != null && !status.isEmpty()) path.append("status=").append(status).append("&");
            if (stationId != null && !stationId.isEmpty()) path.append("stationId=").append(stationId);
            return parseList(api.requestArray("GET", path.toString(), null, requireToken()));
        }, callback);
    }

    // ===== TRANSACTION =====
    public void transaction(String id, Callback<Reservation> callback) {
        run(() -> {
            JSONObject response = api.request("GET", "reservations/" + id + "/transaction", null, requireToken());
            return new Reservation(response);
        }, callback);
    }

    // ===== Helpers =====
    private String requireToken() throws ApiException {
        String token = accounts.sessionToken();
        if (token == null || !accounts.signedIn())
            throw new ApiException(401, "Session has ended. Please sign in again.");
        return token;
    }
    private String requireNic() throws ApiException {
        String nic = accounts.sessionNic();
        if (nic == null || !accounts.signedIn())
            throw new ApiException(401, "Session has ended. Please sign in again.");
        return nic;
    }
    private static List<Reservation> parseList(JSONArray array) throws JSONException {
        List<Reservation> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(new Reservation(array.getJSONObject(i)));
        return list;
    }

    private <T> void run(Work<T> work, Callback<T> callback) {
        executor.execute(() -> {
            try {
                T result = work.run();
                main.post(() -> callback.success(result));
            } catch (ApiException e) {
                main.post(() -> callback.failure(e.status, e.getMessage()));
            } catch (SocketTimeoutException e) {
                main.post(() -> callback.failure(0, "Request timed out. Please try again."));
            } catch (java.io.IOException e) {
                main.post(() -> callback.failure(0, "Network error. Check your connection."));
            } catch (Exception e) {
                main.post(() -> callback.failure(0, "Unexpected response from server."));
            }
        });
    }
}