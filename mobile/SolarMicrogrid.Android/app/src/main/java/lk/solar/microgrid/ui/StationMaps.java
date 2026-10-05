package lk.solar.microgrid.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import lk.solar.microgrid.R;
import lk.solar.microgrid.data.Station;

/** Opens the station's server-provided coordinates in Google Maps. No SDK key is required. */
final class StationMaps {
    private StationMaps() { }

    static void open(Activity activity, Station station) {
        Uri location = Uri.parse("https://www.google.com/maps/search/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("query", station.latitude + "," + station.longitude)
                .build();
        Intent intent = new Intent(Intent.ACTION_VIEW, location);
        try {
            activity.startActivity(intent.setPackage("com.google.android.apps.maps"));
        } catch (ActivityNotFoundException noMapsApp) {
            try {
                activity.startActivity(intent.setPackage(null));
            } catch (ActivityNotFoundException noBrowser) {
                Toast.makeText(activity, R.string.map_open_unavailable, Toast.LENGTH_LONG).show();
            }
        }
    }
}
