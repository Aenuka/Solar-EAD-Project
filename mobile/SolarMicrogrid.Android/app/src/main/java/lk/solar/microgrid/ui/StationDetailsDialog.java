package lk.solar.microgrid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lk.solar.microgrid.R;
import lk.solar.microgrid.data.Station;

/** Read-only station overview with scrollable availability and a persistent close action. */
final class StationDetailsDialog {
    private final Activity activity;
    private static final int INK = SolarStyle.INK;
    private static final int MUTED = SolarStyle.MUTED;
    private static final int GREEN = SolarStyle.GREEN;

    // Keeps the hosting activity for displaying the dialog. *****
    private StationDetailsDialog(Activity activity) { this.activity = activity; }

    // Opens the details dialog for the selected station. *****
    static void show(Activity activity, Station station) throws JSONException {
        new StationDetailsDialog(activity).open(station);
    }

    // Builds station details and bookable energy windows from the API response. *****
    private void open(Station station) throws JSONException {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = column(20);
        root.setBackground(background(Color.WHITE, 24));
        TextView eyebrow = text("MICROGRID STATION", 11, GREEN, true);
        root.addView(eyebrow);
        root.addView(text(station.name, 24, INK, true));
        root.addView(text(station.address, 14, MUTED, false));

        Button map = new Button(activity);
        map.setText(R.string.view_on_google_maps);
        SolarStyle.button(map, false);
        map.setOnClickListener(v -> StationMaps.open(activity, station));
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, -2);
        mapParams.topMargin = dp(8);
        mapParams.bottomMargin = dp(12);
        root.addView(map, mapParams);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout body = column(0);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout metrics = new LinearLayout(activity);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metric(metrics, number(station.capacityKw) + " kW", "Power");
        metric(metrics, number(station.storageKwh) + " kWh", "Storage");
        metric(metrics, String.valueOf(station.batterySlots), "Battery slots");
        body.addView(metrics);

        body.addView(text("Operating hours", 16, INK, true));
        JSONObject schedule = station.source.getJSONObject("schedule");
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        JSONArray days = schedule.getJSONArray("days");
        StringBuilder dayLabels = new StringBuilder();
        for (int i = 0; i < days.length(); i++) {
            int day = days.getInt(i);
            if (day < 0 || day > 6) throw new JSONException("Invalid operating day");
            if (i > 0) dayLabels.append(" · ");
            dayLabels.append(names[day]);
        }
        body.addView(text(dayLabels.toString(), 14, MUTED, false));
        body.addView(text(schedule.getString("opensAt") + " – " + schedule.getString("closesAt") + "  ·  Sri Lanka time", 14, INK, false));
        body.addView(text("Available energy windows", 18, INK, true));

        JSONArray slots = station.source.getJSONArray("slots");
        if (slots.length() == 0) body.addView(text("No upcoming windows yet. Check back later for availability.", 14, MUTED, false));
        DateTimeFormatter date = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 0; i < slots.length(); i++) {
            JSONObject slot = slots.getJSONObject(i);
            OffsetDateTime start = local(slot.getString("startsAt"));
            OffsetDateTime end = local(slot.getString("endsAt"));
            LinearLayout card = column(14);
            card.setBackground(background(SolarStyle.BACKGROUND, 14));
            LinearLayout.LayoutParams spacing = new LinearLayout.LayoutParams(-1, -2);
            spacing.setMargins(0, dp(8), 0, dp(4));
            body.addView(card, spacing);
            card.addView(text(start.format(date), 14, INK, true));
            card.addView(text(start.format(time) + " – " + (start.toLocalDate().equals(end.toLocalDate()) ? "" : end.format(date) + " ") + end.format(time), 20, INK, true));
            int available = slot.getInt("availableSlots");
            double energy = slot.getDouble("availableEnergyKwh");
            card.addView(text(available + " battery slots  ·  " + number(energy) + " kWh available", 14, GREEN, false));
            if (available == 0 || energy <= 0) {
                card.addView(text("No availability remaining", 12, MUTED, true));
            } else {
                Button book = new Button(activity);
                book.setText("Book this slot");
                book.setAllCaps(false);
                book.setTextColor(GREEN);
                book.setBackground(background(Color.TRANSPARENT, 12));
                book.setOnClickListener(v -> {
                    dialog.dismiss();
                    android.content.Intent intent = new android.content.Intent(activity, CreateBookingActivity.class);
                    intent.putExtra("stationId", station.id);
                    try {
                        intent.putExtra("slotId", slot.getString("id"));
                        intent.putExtra("startsAt", slot.getString("startsAt"));
                        intent.putExtra("endsAt", slot.getString("endsAt"));
                        intent.putExtra("stationName", station.name);
                        intent.putExtra("availableEnergyKwh", slot.getDouble("availableEnergyKwh"));
                    } catch (JSONException e) {}
                    activity.startActivity(intent);
                });
                card.addView(book, new LinearLayout.LayoutParams(-1, dp(40)));
            }
        }
        body.addView(text("Availability is confirmed when you book.", 12, MUTED, false));

        Button close = new Button(activity);
        close.setText("Done");
        close.setAllCaps(false);
        close.setTextColor(Color.WHITE);
        close.setTextSize(16);
        close.setBackground(background(GREEN, 12));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(48));
        closeParams.topMargin = dp(12);
        root.addView(close, closeParams);
        close.setOnClickListener(view -> dialog.dismiss());
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            android.util.DisplayMetrics display = activity.getResources().getDisplayMetrics();
            window.setLayout(Math.min(display.widthPixels - dp(32), dp(560)), (int) (display.heightPixels * 0.85));
        }
        dialog.show();
    }

    // Converts an API timestamp to Sri Lanka local time for display. *****
    private OffsetDateTime local(String value) {
         return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30));
    }
    // Formats a numeric station value to at most two decimal places. *****
    private String number(double value) { NumberFormat format = NumberFormat.getNumberInstance(); format.setMaximumFractionDigits(2); return format.format(value); }
    // Converts density-independent units to screen pixels. *****
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    // Creates a padded vertical container for dialog content. *****
    private LinearLayout column(int padding) { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL); view.setPadding(dp(padding), dp(padding), dp(padding), dp(padding)); return view; }
    // Creates a rounded background for a dialog element. *****
    private GradientDrawable background(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    // Creates consistently styled text for station details. *****
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(5), 0, dp(5));
        if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return view;
    }
    // Adds one labeled capacity or availability metric to a row. *****
    private void metric(LinearLayout row, String value, String label) {
        LinearLayout cell = column(6);
        cell.addView(text(value, 16, INK, true));
        cell.addView(text(label, 11, MUTED, false));
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
    }
}
