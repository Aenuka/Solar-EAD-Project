/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Handles Grid Operator login, role checks, in-memory JWT sessions, logout, and authenticated operator API requests.
 */

package lk.solar.microgrid.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import lk.solar.microgrid.R;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OperatorRepository {
    private final ApiClient api;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private volatile String token, operatorId, fullName, role, username;
    private volatile Instant expiresAt;

    public interface Callback<T> { void success(T result); void failure(int status, String message); }
    private interface Work<T> { T run() throws Exception; }

    public OperatorRepository(Context context, ApiClient api) {
        this.context = context.getApplicationContext();
        this.api = api;
    }

    public boolean signedIn() { return token != null && expiresAt != null && expiresAt.isAfter(Instant.now()); }

    public String getFullName() { return fullName; }
    public String getUsername() { return username; }

    public void login(String username, String password, Callback<Void> callback) {
        run(() -> {
            JSONObject body = new JSONObject().put("username", username.trim()).put("password", password);
            JSONObject response = api.request("POST", "auth/staff/login", body, null);
            
            String responseRole = response.getString("role");
            if (!"GridOperator".equals(responseRole)) {
                if ("Backoffice".equals(responseRole)) {
                    throw new ApiException(403, "Mobile access is restricted to Grid Operators.");
                }
                throw new JSONException("Unexpected session response");
            }
            if (!"Bearer".equals(response.getString("tokenType"))) {
                throw new JSONException("Unexpected token type");
            }

            token = response.getString("accessToken");
            operatorId = response.getString("id");
            fullName = response.getString("fullName");
            this.username = username.trim();
            role = responseRole;
            expiresAt = OffsetDateTime.parse(response.getString("expiresAt")).toInstant();
            return null;
        }, callback);
    }

    public void logout() {
        token = null; operatorId = null; fullName = null; role = null; expiresAt = null; username = null;
    }

    public void verifyTransaction(String transactionToken, Callback<Reservation> callback) {
        run(() -> {
            if (!signedIn()) throw new ApiException(401, "Operator session has expired.");
            try {
                JSONObject response = api.request("GET", "Reservations/verify?token=" + java.net.URLEncoder.encode(transactionToken, "UTF-8"), null, token);
                return new Reservation(response);
            } catch (ApiException e) {
                if (e.status == 403) {
                    throw new ApiException(403, "Grid Operator authorization required.");
                }
                throw e;
            }
        }, callback);
    }

    public void completeTransaction(String id, Callback<Reservation> callback) {
        run(() -> {
            if (!signedIn()) throw new ApiException(401, "Operator session has expired.");
            try {
                JSONObject response = api.request("PATCH", "Reservations/" + java.net.URLEncoder.encode(id, "UTF-8") + "/complete", null, token);
                return new Reservation(response);
            } catch (ApiException e) {
                if (e.status == 403) {
                    throw new ApiException(403, "Grid Operator authorization required.");
                }
                throw e;
            }
        }, callback);
    }

    public void loadDashboard(Callback<OperatorDashboard> callback) {
        run(() -> {
            if (!signedIn()) throw new ApiException(401, "Operator session has expired.");
            try {
                JSONObject response = api.request("GET", "Reservations/dashboard", null, token);
                return new OperatorDashboard(response);
            } catch (ApiException e) {
                if (e.status == 403) {
                    throw new ApiException(403, "Grid Operator authorization required.");
                }
                throw e;
            }
        }, callback);
    }

    public void searchCompletedOperations(Callback<java.util.List<Reservation>> callback) {
        run(() -> {
            if (!signedIn()) throw new ApiException(401, "Operator session has expired.");
            try {
                org.json.JSONArray response = api.requestArray("GET", "Reservations/search?status=COMPLETED", null, token);
                java.util.List<Reservation> list = new java.util.ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    list.add(new Reservation(response.getJSONObject(i)));
                }
                // Sort by completedAt descending
                list.sort((a, b) -> {
                    String aDate = a.source.optString("completedAt", a.source.optString("updatedAt", ""));
                    String bDate = b.source.optString("completedAt", b.source.optString("updatedAt", ""));
                    return bDate.compareTo(aDate);
                });
                return list;
            } catch (ApiException e) {
                if (e.status == 403) {
                    throw new ApiException(403, "Grid Operator authorization required.");
                }
                throw e;
            }
        }, callback);
    }

    private <T> void run(Work<T> work, Callback<T> callback) {
        executor.execute(() -> {
            try { T result = work.run(); main.post(() -> callback.success(result)); }
            catch (ApiException e) {
                if (e.status == 401) logout();
                main.post(() -> callback.failure(e.status, e.getMessage()));
            }
            catch (SocketTimeoutException e) { fail(callback, R.string.timeout_error); }
            catch (IOException e) { fail(callback, R.string.network_error); }
            catch (Exception e) { fail(callback, R.string.contract_error); }
        });
    }
    private <T> void fail(Callback<T> callback, int messageId) { main.post(() -> callback.failure(0, context.getString(messageId))); }
}
