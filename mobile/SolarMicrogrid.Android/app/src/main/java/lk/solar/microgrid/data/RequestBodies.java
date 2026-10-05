/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Builds JSON request bodies for prosumer login, registration, profile updates, and deactivation.
 */

package lk.solar.microgrid.data;

import org.json.JSONException;
import org.json.JSONObject;

/** Keep these field names aligned with SolarMicrogrid.Contracts and docs/openapi.json. */
public final class RequestBodies {
    private RequestBodies() { }
    public static JSONObject login(String nic, String password) throws JSONException {
        return new JSONObject().put("nic", nic.trim()).put("password", password);
    }
    public static JSONObject register(String nic, String fullName, String email, String phone, String address, String password) throws JSONException {
        return new JSONObject().put("nic", nic.trim()).put("fullName", fullName.trim()).put("email", email.trim())
            .put("phone", phone.trim()).put("address", address.trim()).put("password", password);
    }
    public static JSONObject profile(String fullName, String email, String phone, String address, long version) throws JSONException {
        return new JSONObject().put("fullName", fullName.trim()).put("email", email.trim())
            .put("phone", phone.trim()).put("address", address.trim()).put("version", version);
    }
    public static JSONObject deactivate(String reason, long version) throws JSONException {
        return new JSONObject().put("reason", reason.trim()).put("version", version);
    }
}
