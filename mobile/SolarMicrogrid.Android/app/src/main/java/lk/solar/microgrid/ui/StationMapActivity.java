package lk.solar.microgrid.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.List;
import lk.solar.microgrid.BuildConfig;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.Station;
import org.json.JSONArray;
import org.json.JSONObject;

/** Pure Android UI + Google Maps SDK. Station coordinates and inventory come exclusively from REST. */
public final class StationMapActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Station> stations = new ArrayList<>();
    private AccountRepository accounts;
    private MapView mapView;
    private GoogleMap map;
    private LinearLayout results;
    private TextView status, mapStatus;
    private EditText latitude, longitude;
    private Button previous, next;
    private LocationManager locations;
    private LocationListener listener;
    private int page = 1, generation = 0, detailGeneration = 0;
    private String filter = "";
    private final Runnable locationTimeout = () -> { stopLocation(); status.setText(R.string.location_unavailable); };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        accounts = ((SolarApplication)getApplication()).accounts();
        if (!accounts.signedIn()) { finish(); return; }
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 12, 20, 12);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left + 20, bars.top, bars.right + 20, bars.bottom);
            }
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
        addButton(root, R.string.back_account, this::finish);
        TextView heading = new TextView(this); heading.setText(R.string.nearby_stations); heading.setTextSize(24); root.addView(heading);
        status = new TextView(this); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); root.addView(status);
        LinearLayout coordinates = new LinearLayout(this);
        latitude = coordinate(coordinates, R.string.station_latitude); longitude = coordinate(coordinates, R.string.station_longitude); root.addView(coordinates);
        LinearLayout search = new LinearLayout(this);
        addButton(search, R.string.search_coordinates, this::searchCoordinates);
        addButton(search, R.string.use_location, this::requestLocation);
        root.addView(search);
        LinearLayout tools = new LinearLayout(this);
        addButton(tools, R.string.all_stations, () -> { filter = ""; page = 1; load(); });
        addButton(tools, R.string.refresh, this::load);
        root.addView(tools);
        mapStatus = new TextView(this); root.addView(mapStatus);
        if (BuildConfig.MAPS_CONFIGURED && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            try {
                mapView = new MapView(this);
                root.addView(mapView, new LinearLayout.LayoutParams(-1, 0, 1));
                mapView.onCreate(state == null ? null : state.getBundle("map"));
                mapStatus.setText(R.string.map_loading);
                mapView.getMapAsync(ready -> {
                    if (!alive()) return;
                    map = ready;
                    map.setOnMarkerClickListener(marker -> { if (marker.getTag() instanceof String) detail((String)marker.getTag()); return true; });
                    map.setOnMapLoadedCallback(() -> { if (alive()) mapStatus.setVisibility(View.GONE); });
                    renderMarkers();
                });
            } catch (RuntimeException e) {
                if (mapView != null) root.removeView(mapView);
                mapView = null; mapStatus.setText(R.string.map_unavailable);
            }
        } else mapStatus.setText(R.string.map_unavailable);
        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout pages = new LinearLayout(this);
        previous = addButton(pages, R.string.previous_stations, () -> { page--; load(); });
        next = addButton(pages, R.string.next_stations, () -> { page++; load(); });
        root.addView(pages);
        if (state != null) {
            filter = state.getString("filter", ""); page = state.getInt("page", 1);
            latitude.setText(state.getString("latitude", "")); longitude.setText(state.getString("longitude", ""));
        }
        load();
    }
    private Button addButton(LinearLayout root, int label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        root.addView(button, root.getOrientation() == LinearLayout.HORIZONTAL ? new LinearLayout.LayoutParams(0, -2, 1) : new LinearLayout.LayoutParams(-1, -2));
        return button;
    }
    private EditText coordinate(LinearLayout parent, int hint) {
        EditText value = new EditText(this); value.setHint(hint); value.setContentDescription(getString(hint));
        value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        parent.addView(value, new LinearLayout.LayoutParams(0, -2, 1)); return value;
    }
    private void searchCoordinates() {
        try {
            double lat = Double.parseDouble(latitude.getText().toString()); double lon = Double.parseDouble(longitude.getText().toString());
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
            filter = "&latitude=" + lat + "&longitude=" + lon + "&radiusKm=25"; page = 1; load();
        } catch (NumberFormatException e) { status.setText(R.string.invalid_coordinates); }
    }
    private void load() {
        final int requestGeneration = ++generation;
        detailGeneration++;
        stations.clear(); results.removeAllViews(); if (map != null) map.clear();
        previous.setEnabled(false); next.setEnabled(false); status.setText(R.string.working);
        accounts.stations("activeOnly=true&pageSize=20&page=" + page + filter, new AccountRepository.Callback<>() {
            @Override public void success(JSONObject response) {
                if (!alive() || generation != requestGeneration) return;
                try {
                    JSONArray items = response.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) stations.add(new Station(items.getJSONObject(i)));
                    for (Station station : stations) {
                        Button button = new Button(StationMapActivity.this); button.setAllCaps(false);
                        button.setText(getString(R.string.station_list_item, station.name, station.summary()));
                        button.setOnClickListener(v -> detail(station.id)); results.addView(button);
                    }
                    int total = response.getInt("total");
                    status.setText(getResources().getQuantityString(filter.isEmpty() ? R.plurals.station_results : R.plurals.nearby_results, total, total, page));
                    previous.setEnabled(page > 1); next.setEnabled(page * 20 < response.getInt("total"));
                    renderMarkers();
                } catch (Exception e) { stations.clear(); results.removeAllViews(); status.setText(R.string.contract_error); }
            }
            @Override public void failure(int code, String message) { if (alive() && generation == requestGeneration) error(code, message); }
        });
    }
    private void renderMarkers() {
        if (map == null || stations.isEmpty()) return;
        map.clear(); LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (Station station : stations) {
            LatLng position = new LatLng(station.latitude, station.longitude); bounds.include(position);
            Marker marker = map.addMarker(new MarkerOptions().position(position).title(station.name).snippet(station.summary()));
            if (marker != null) marker.setTag(station.id);
        }
        mapView.post(() -> {
            if (!alive() || map == null || stations.isEmpty()) return;
            try { map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 60)); }
            catch (IllegalStateException e) { map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(stations.get(0).latitude, stations.get(0).longitude))); }
        });
    }
    private void detail(String id) {
        int requestGeneration = ++detailGeneration;
        status.setText(R.string.working);
        accounts.station(id, new AccountRepository.Callback<>() {
            @Override public void success(JSONObject result) {
                if (!alive() || requestGeneration != detailGeneration) return;
                try {
                    Station station = new Station(result);
                    TextView text = new TextView(StationMapActivity.this); text.setText(station.details()); text.setPadding(24, 16, 24, 16);
                    ScrollView scroll = new ScrollView(StationMapActivity.this); scroll.addView(text);
                    new AlertDialog.Builder(StationMapActivity.this).setTitle(R.string.station_details).setView(scroll).setPositiveButton(android.R.string.ok, null).show();
                    status.setText(R.string.station_detail_loaded);
                } catch (Exception e) { status.setText(R.string.contract_error); }
            }
            @Override public void failure(int code, String message) { if (alive() && requestGeneration == detailGeneration) error(code, message); }
        });
    }
    private void error(int code, String message) {
        status.setText(message);
        if (code == 401) new AlertDialog.Builder(this).setMessage(message).setCancelable(false).setPositiveButton(android.R.string.ok, (d, w) -> {
            startActivity(new android.content.Intent(this, MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish();
        }).show();
    }
    private void requestLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 42); return;
        }
        locate();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == 42) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate();
            else status.setText(R.string.location_denied);
        }
    }
    @SuppressWarnings("MissingPermission")
    private void locate() {
        stopLocation(); locations = (LocationManager)getSystemService(LOCATION_SERVICE);
        if (locations == null) { status.setText(R.string.location_unavailable); return; }
        status.setText(R.string.location_loading);
        listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (!alive()) return;
                stopLocation(); latitude.setText(String.valueOf(location.getLatitude())); longitude.setText(String.valueOf(location.getLongitude())); searchCoordinates();
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };
        try {
            boolean requested = false;
            for (String provider : new String[] {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
                if (!locations.isProviderEnabled(provider)) continue;
                if (provider.equals(LocationManager.GPS_PROVIDER) && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) continue;
                locations.requestLocationUpdates(provider, 0, 0, listener, Looper.getMainLooper()); requested = true;
            }
            if (requested) handler.postDelayed(locationTimeout, 15000);
            else { stopLocation(); status.setText(R.string.location_unavailable); }
        } catch (SecurityException | IllegalArgumentException e) { stopLocation(); status.setText(R.string.location_unavailable); }
    }
    private void stopLocation() {
        handler.removeCallbacks(locationTimeout);
        if (locations != null && listener != null) {
            try { locations.removeUpdates(listener); } catch (SecurityException ignored) { /* Permission may have been revoked. */ }
        }
        listener = null;
    }
    private boolean alive() { return !isFinishing() && !isDestroyed(); }
    @Override protected void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override protected void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { stopLocation(); if (mapView != null) mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Bundle mapState = new Bundle(); if (mapView != null) mapView.onSaveInstanceState(mapState); state.putBundle("map", mapState);
        state.putString("filter", filter); state.putInt("page", page);
        state.putString("latitude", latitude.getText().toString()); state.putString("longitude", longitude.getText().toString());
    }
}
