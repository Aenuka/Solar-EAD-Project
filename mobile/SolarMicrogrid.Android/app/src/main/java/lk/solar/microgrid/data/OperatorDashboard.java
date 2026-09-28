package lk.solar.microgrid.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OperatorDashboard {
    public final int pendingCount;
    public final int approvedFutureCount;
    public final int completedCount;
    public final List<Reservation> pendingReservations;
    public final List<Reservation> recentCompletedReservations;

    public OperatorDashboard(JSONObject source) throws org.json.JSONException {
        this.pendingCount = source.optInt("pendingCount");
        this.approvedFutureCount = source.optInt("approvedFutureCount");
        this.completedCount = source.optInt("completedCount");
        
        this.pendingReservations = new ArrayList<>();
        JSONArray pendingArray = source.optJSONArray("pendingReservations");
        if (pendingArray != null) {
            for (int i = 0; i < pendingArray.length(); i++) {
                this.pendingReservations.add(new Reservation(pendingArray.optJSONObject(i)));
            }
        }
        
        this.recentCompletedReservations = new ArrayList<>();
        JSONArray completedArray = source.optJSONArray("recentCompletedReservations");
        if (completedArray != null) {
            for (int i = 0; i < completedArray.length(); i++) {
                this.recentCompletedReservations.add(new Reservation(completedArray.optJSONObject(i)));
            }
        }
    }
}
