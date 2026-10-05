package lk.solar.microgrid.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.List;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.Station;
import org.json.JSONArray;
import org.json.JSONObject;

/** Google Maps viewer. Station coordinates and inventory come exclusively from REST. */
public final class StationMapActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN;
    private static final int INK = SolarStyle.INK;
    private static final int MUTED = SolarStyle.MUTED;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Station> stations = new ArrayList<>();
    private AccountRepository accounts;
    private MapView mapView;
    private GoogleMap googleMap;
    private LatLng currentLocation;
    private LinearLayout results;
    private TextView status, mapStatus;
    private Button previous, next, retry, reset;
    private LocationManager locations;
    private LocationListener listener;
    private int page = 1, generation = 0, detailGeneration = 0;
    private String filter = "";
    private final Runnable locationTimeout = () -> { stopLocation(); status.setText(R.string.location_unavailable); };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        accounts = ((SolarApplication)getApplication()).accounts();
        if (!accounts.signedIn()) { finish(); return; }
        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setBackgroundColor(SolarStyle.BACKGROUND);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(28));
        screen.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(screen);
        SolarStyle.hero(root, "Explore", "Find your next energy connection.");
        LinearLayout search = new LinearLayout(this);
        addButton(search, R.string.use_location, this::requestLocation);
        reset = new Button(this); reset.setText("All stations"); SolarStyle.button(reset, false);
        reset.setOnClickListener(v -> { filter = ""; page = 1; reset.setVisibility(View.GONE); load(); });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, -2, 1);
        resetParams.setMarginStart(dp(8)); search.addView(reset, resetParams);
        reset.setVisibility(View.GONE);
        root.addView(search);
        mapStatus = text("", 14, MUTED); root.addView(mapStatus);
        try {
            String mapsKey = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                    .metaData.getString("com.google.android.geo.API_KEY", "");
            if (mapsKey.isEmpty() || "YOUR_GOOGLE_MAPS_API_KEY".equals(mapsKey)) throw new IllegalStateException("Maps not configured");
            mapView = new MapView(this);
            mapView.onCreate(state);
            mapView.getMapAsync(map -> {
                if (!alive()) return;
                googleMap = map;
                mapStatus.setText("Tap a pin to view station details.");
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.setOnMarkerClickListener(marker -> {
                    Object stationId = marker.getTag();
                    if (stationId instanceof String) { detail((String) stationId); return true; }
                    return false;
                });
                renderMarkers();
            });
            android.widget.FrameLayout mapCard = new android.widget.FrameLayout(this) {
                @Override public boolean dispatchTouchEvent(android.view.MotionEvent event) {
                    getParent().requestDisallowInterceptTouchEvent(event.getActionMasked() != android.view.MotionEvent.ACTION_UP
                            && event.getActionMasked() != android.view.MotionEvent.ACTION_CANCEL);
                    return super.dispatchTouchEvent(event);
                }
            };
            SolarStyle.card(mapCard); mapCard.setClipToOutline(true);
            mapCard.addView(mapView, new android.widget.FrameLayout.LayoutParams(-1, dp(240)));
            LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, -2);
            mapParams.topMargin = dp(12); mapParams.bottomMargin = dp(12);
            root.addView(mapCard, mapParams);
            mapStatus.setText(R.string.map_loading);
        } catch (RuntimeException | PackageManager.NameNotFoundException e) {
            if (mapView != null) { root.removeView(mapView); mapView.onDestroy(); }
            mapView = null; mapStatus.setText(R.string.map_unavailable);
        }
        SolarStyle.section(root, "Stations");
        status = text("", 14, MUTED);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results, new LinearLayout.LayoutParams(-1, -2));
        retry = new Button(this); retry.setText("Try again"); SolarStyle.button(retry, false);
        retry.setOnClickListener(v -> load()); retry.setVisibility(View.GONE);
        root.addView(retry, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout pages = new LinearLayout(this);
        previous = addButton(pages, R.string.previous_stations, () -> { page--; load(); });
        next = addButton(pages, R.string.next_stations, () -> { page++; load(); });
        root.addView(pages);
        if (state != null) {
            filter = state.getString("filter", ""); page = state.getInt("page", 1);
            if (state.containsKey("locationLatitude")) {
                currentLocation = new LatLng(state.getDouble("locationLatitude"), state.getDouble("locationLongitude"));
            }
        }
        reset.setVisibility(filter.isEmpty() ? View.GONE : View.VISIBLE);
        load();
    }
    private Button addButton(LinearLayout root, int label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        styleButton(button);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = root.getOrientation() == LinearLayout.HORIZONTAL ? new LinearLayout.LayoutParams(0, -2, 1) : new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        root.addView(button, params);
        return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(4), 0, dp(4)); return view;
    }
    private void styleButton(Button button) { SolarStyle.button(button, false); }
    private void addStationCard(Station station) {
        LinearLayout card = SolarStyle.group(results);
        SolarStyle.row(card, R.drawable.ic_nav_bolt, station.name, station.address + "\n" + station.summary(), () -> detail(station.id));
    }
    private void load() {
        final int requestGeneration = ++generation;
        detailGeneration++;
        stations.clear(); results.removeAllViews();
        if (googleMap != null) { googleMap.clear(); addCurrentLocationMarker(); }
        previous.setEnabled(false); next.setEnabled(false); retry.setVisibility(View.GONE); status.setText(R.string.working);
        accounts.stations("activeOnly=true&pageSize=20&page=" + page + filter, new AccountRepository.Callback<>() {
            @Override public void success(JSONObject response) {
                if (!alive() || generation != requestGeneration) return;
                try {
                    JSONArray items = response.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) stations.add(new Station(items.getJSONObject(i)));
                    for (Station station : stations) {
                        addStationCard(station);
                    }
                    int total = response.getInt("total");
                    if (stations.isEmpty()) results.addView(text(getString(R.string.no_stations), 16, MUTED));
                    status.setText(getResources().getQuantityString(filter.isEmpty() ? R.plurals.station_results : R.plurals.nearby_results, total, total, page));
                    previous.setEnabled(page > 1); next.setEnabled(page * 20 < response.getInt("total"));
                    renderMarkers();
                } catch (Exception e) { stations.clear(); results.removeAllViews(); status.setText(R.string.contract_error); }
            }
            @Override public void failure(int code, String message) { if (alive() && generation == requestGeneration) error(code, message); }
        });
    }
    private void renderMarkers() {
        if (googleMap == null) return;
        googleMap.clear();
        List<LatLng> points = new ArrayList<>();
        for (Station station : stations) {
            LatLng position = new LatLng(station.latitude, station.longitude); points.add(position);
            var marker = googleMap.addMarker(new MarkerOptions().position(position)
                .title(station.name).snippet(station.summary()));
            if (marker != null) marker.setTag(station.id);
        }
        addCurrentLocationMarker();
        if (currentLocation != null) points.add(currentLocation);
        if (points.isEmpty()) return;
        final int renderedGeneration = generation;
        mapView.post(() -> {
            if (!alive() || googleMap == null || generation != renderedGeneration) return;
            if (points.size() == 1) googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
            else {
                LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                for (LatLng point : points) bounds.include(point);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), dp(48)));
            }
        });
    }
    private void addCurrentLocationMarker() {
        if (googleMap == null || currentLocation == null) return;
        googleMap.addMarker(new MarkerOptions().position(currentLocation)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .title(getString(R.string.your_location)));
        mapStatus.setText(R.string.location_map_hint);
    }
    private void detail(String id) {
        int requestGeneration = ++detailGeneration;
        status.setText(R.string.working);
        accounts.station(id, new AccountRepository.Callback<>() {
            @Override public void success(JSONObject result) {
                if (!alive() || requestGeneration != detailGeneration) return;
                try {
                    Station station = new Station(result);
                    StationDetailsDialog.show(StationMapActivity.this, station);
                    status.setText(R.string.station_detail_loaded);
                } catch (Exception e) { status.setText(R.string.contract_error); }
            }
            @Override public void failure(int code, String message) { if (alive() && requestGeneration == detailGeneration) error(code, message); }
        });
    }
    private void error(int code, String message) {
        status.setText(message);
        retry.setVisibility(View.VISIBLE);
        if (code == 401) new AlertDialog.Builder(this).setMessage(message).setCancelable(false).setPositiveButton(android.R.string.ok, (d, w) -> {
            startActivity(new android.content.Intent(this, MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)); finish();
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
                stopLocation();
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                filter = "&latitude=" + location.getLatitude() + "&longitude=" + location.getLongitude() + "&radiusKm=25";
                reset.setVisibility(View.VISIBLE);
                page = 1; load();
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
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); googleMap = null; if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mapView != null) mapView.onSaveInstanceState(state);
        state.putString("filter", filter); state.putInt("page", page);
        if (currentLocation != null) {
            state.putDouble("locationLatitude", currentLocation.latitude);
            state.putDouble("locationLongitude", currentLocation.longitude);
        }
    }
}
