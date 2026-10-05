package lk.solar.microgrid.data;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Loads every station page for a named selection, rather than truncating choices. */
public final class StationChoices {
    private StationChoices() { }
    public static void load(AccountRepository accounts, AccountRepository.Callback<List<Station>> callback) {
        loadPage(accounts, 1, new ArrayList<>(), callback);
    }
    private static void loadPage(AccountRepository accounts, int page, List<Station> choices,
            AccountRepository.Callback<List<Station>> callback) {
        accounts.stations("activeOnly=true&pageSize=100&page=" + page, new AccountRepository.Callback<JSONObject>() {
            @Override public void success(JSONObject response) {
                try {
                    JSONArray items = response.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) choices.add(new Station(items.getJSONObject(i)));
                    if (choices.size() < response.getInt("total") && items.length() > 0)
                        loadPage(accounts, page + 1, choices, callback);
                    else callback.success(choices);
                } catch (Exception e) { callback.failure(0, "Station details are unavailable. Please try again."); }
            }
            @Override public void failure(int status, String message) { callback.failure(status, message); }
        });
    }
}
