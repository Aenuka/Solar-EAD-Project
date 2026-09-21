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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates API and cache. Every state change is confirmed by the server before cache updates. */
public final class AccountRepository {
    private final ApiClient api;
    private final ProfileCache cache;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    // In-memory session: process termination requires another online sign-in.
    private volatile String token, nic;
    private volatile Instant expiresAt;

    public interface Callback<T> { void success(T result); void failure(int status, String message); }
    private interface Work<T> { T run() throws Exception; }

    public AccountRepository(Context context, String baseUrl) {
        this.context = context.getApplicationContext();
        api = new ApiClient(baseUrl);
        cache = new ProfileCache(this.context);
    }
    public boolean signedIn() { return token != null && expiresAt != null && expiresAt.isAfter(Instant.now()); }

    public void login(String id, String password, Callback<ProfileCache.Snapshot> callback) {
        run(() -> { loginOnline(id, password); return load(); }, callback);
    }
    public void register(String id, String name, String email, String phone, String address, String password, Callback<ProfileCache.Snapshot> callback) {
        run(() -> {
            api.request("POST", "prosumers", RequestBodies.register(id, name, email, phone, address, password), null);
            loginOnline(id, password);
            return load();
        }, callback, R.string.registration_timeout_error);
    }
    private void loginOnline(String id, String password) throws Exception {
        JSONObject response = api.request("POST", "auth/prosumers/login", RequestBodies.login(id, password), null);
        if (!"Prosumer".equals(response.getString("role")) || !"Bearer".equals(response.getString("tokenType")))
            throw new JSONException("Unexpected session response");
        String newToken = response.getString("accessToken");
        String newNic = response.getString("id");
        Instant newExpiry = Instant.parse(response.getString("expiresAt"));
        cache.clear();
        token = newToken; nic = newNic; expiresAt = newExpiry;
    }
    public void refresh(Callback<ProfileCache.Snapshot> callback) { run(this::load, callback); }

    private ProfileCache.Snapshot load() throws Exception {
        requireSession();
        try { return confirmed(api.request("GET", "prosumers/me", null, token)); }
        catch (ApiException e) {
            // The API can still be reachable while its cloud database is unavailable.
            // Authentication/authorization failures must never be hidden by cached data.
            if (e.status >= 500 && e.status <= 599) {
                ProfileCache.Snapshot local = cache.read(nic);
                if (local != null) return local;
            }
            throw e;
        }
        catch (IOException e) {
            ProfileCache.Snapshot local = cache.read(nic);
            if (local != null) return local;
            throw e;
        }
    }
    public void save(String name, String email, String phone, String address, long version, Callback<ProfileCache.Snapshot> callback) {
        run(() -> { requireSession(); return confirmed(api.request("PATCH", "prosumers/me", RequestBodies.profile(name, email, phone, address, version), token)); }, callback, R.string.change_timeout_error);
    }
    public void deactivate(String reason, long version, Callback<ProfileCache.Snapshot> callback) {
        run(() -> { requireSession(); return confirmed(api.request("POST", "prosumers/me/deactivation-requests", RequestBodies.deactivate(reason, version), token)); }, callback, R.string.change_timeout_error);
    }
    public void logout(boolean allDevices, Callback<Void> callback) {
        run(() -> {
            if (allDevices && signedIn()) {
                try { api.request("POST", "auth/logout", null, token); }
                catch (ApiException e) { if (e.status != 401) throw e; }
            }
            clearSession(); return null;
        }, callback);
    }
    private void requireSession() throws ApiException {
        if (!signedIn()) throw new ApiException(401, context.getString(R.string.session_ended));
    }
    private ProfileCache.Snapshot confirmed(JSONObject response) throws JSONException {
        Profile profile = new Profile(response);
        if (!profile.nic.equals(nic)) throw new JSONException("Profile/session mismatch");
        cache.save(profile);
        return new ProfileCache.Snapshot(profile, false, System.currentTimeMillis());
    }
    private void clearSession() { token = null; nic = null; expiresAt = null; cache.clear(); }

    private <T> void run(Work<T> work, Callback<T> callback) {
        run(work, callback, R.string.timeout_error);
    }
    private <T> void run(Work<T> work, Callback<T> callback, int timeoutMessage) {
        executor.execute(() -> {
            try { T result = work.run(); main.post(() -> callback.success(result)); }
            catch (ApiException e) {
                if (e.status == 401) clearSession();
                main.post(() -> callback.failure(e.status, e.getMessage()));
            }
            catch (SocketTimeoutException e) { fail(callback, timeoutMessage); }
            catch (IOException e) { fail(callback, R.string.network_error); }
            catch (Exception e) { fail(callback, R.string.contract_error); }
        });
    }
    private <T> void fail(Callback<T> callback, int messageId) { main.post(() -> callback.failure(0, context.getString(messageId))); }
}
