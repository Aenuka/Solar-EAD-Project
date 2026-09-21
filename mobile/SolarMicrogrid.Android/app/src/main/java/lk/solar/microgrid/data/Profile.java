package lk.solar.microgrid.data;

import org.json.JSONException;
import org.json.JSONObject;

/** Network response model; no credentials or authoritative rules are stored here. */
public final class Profile {
    public final String nic, fullName, email, phone, address, status, requestStatus, requestReason, decisionNote;
    public final long version;
    public final JSONObject json;

    public Profile(JSONObject json) throws JSONException {
        this.json = json;
        nic = json.getString("nic");
        fullName = json.getString("fullName");
        email = json.getString("email");
        phone = json.getString("phone");
        address = json.getString("address");
        status = json.getString("status");
        version = json.getLong("version");
        JSONObject request = json.optJSONObject("deactivationRequest");
        requestStatus = request == null ? null : request.getString("status");
        requestReason = request == null ? null : request.getString("reason");
        decisionNote = request == null || request.isNull("decisionNote") ? null : request.getString("decisionNote");
    }
}
