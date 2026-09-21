package lk.solar.microgrid.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/** HTTP transport only. Account rules and authorization belong to the server. */
public final class ApiClient {
    private final String baseUrl;
    public ApiClient(String baseUrl) { this.baseUrl = baseUrl; }

    public JSONObject request(String method, String path, JSONObject body, String token) throws IOException, JSONException, ApiException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (var output = connection.getOutputStream()) { output.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            }
            int status = connection.getResponseCode();
            if (status == 204) return new JSONObject();
            String text;
            try (InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                text = readBody(input);
            }
            JSONObject response;
            try { response = text.trim().isEmpty() ? new JSONObject() : new JSONObject(text); }
            catch (JSONException e) {
                if (status >= 300) throw new ApiException(status, "Account services are temporarily unavailable.");
                throw e;
            }
            if (status < 200 || status >= 300) throw new ApiException(status, problemMessage(response));
            return response;
        } finally { connection.disconnect(); }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            if (result.size() + length > 1_048_576) throw new IOException("Response too large");
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    static String problemMessage(JSONObject problem) {
        JSONObject errors = problem.optJSONObject("errors");
        if (errors != null) {
            StringBuilder message = new StringBuilder();
            Iterator<String> fields = errors.keys();
            while (fields.hasNext()) {
                JSONArray fieldErrors = errors.optJSONArray(fields.next());
                if (fieldErrors != null) for (int i = 0; i < fieldErrors.length(); i++) {
                    if (message.length() > 0) message.append('\n');
                    message.append(fieldErrors.optString(i));
                }
            }
            if (message.length() > 0) return message.toString();
        }
        return problem.optString("detail", problem.optString("title", "This request could not be completed."));
    }
}
