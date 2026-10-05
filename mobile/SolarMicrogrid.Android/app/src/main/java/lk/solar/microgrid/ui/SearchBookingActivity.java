package lk.solar.microgrid.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import lk.solar.microgrid.R;
import lk.solar.microgrid.data.BookingText;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Search reservations by status and station ID.
 * Author: Sajith
 */
public final class SearchBookingActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN,
            INK = SolarStyle.INK,
            MUTED = SolarStyle.MUTED,
            AMBER = SolarStyle.AMBER,
            RED = SolarStyle.RED;

    private ReservationRepository reservations;
    private LinearLayout content, listContainer;
    private ProgressBar progress;
    private Spinner statusField, stationField;
    private final List<String> stationIds = new ArrayList<>();
    private final String[] statuses = {"", "PENDING", "APPROVED", "CANCELLED", "COMPLETED"};
    private Button searchBtn;
    private final List<Button> actions = new ArrayList<>();

    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        buildUi();
        loadStations();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView heading = text(getString(R.string.search_booking), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(8);
        text(getString(R.string.search_criteria), 14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        text("Booking status", 16, INK, true);
        statusField = new Spinner(this);
        statusField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All statuses", "Pending", "Approved", "Cancelled", "Completed"}));
        statusField.setMinimumHeight(dp(54)); content.addView(statusField, new LinearLayout.LayoutParams(-1, -2));
        text("Station", 16, INK, true);
        stationField = new Spinner(this);
        stationField.setMinimumHeight(dp(54));
        stationIds.add("");
        stationField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"All stations"}));
        content.addView(stationField, new LinearLayout.LayoutParams(-1, -2));

        // Search button
        searchBtn = button(getString(R.string.search_button), true, this::doSearch);

        // Clear button
        button(getString(R.string.clear_button), false, () -> {
            statusField.setSelection(0);
            stationField.setSelection(0);
            listContainer.removeAllViews();
        });

        // Results
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void loadStations() {
        setBusy(true);
        reservations.history(new ReservationRepository.Callback<List<Reservation>>() {
            @Override public void success(List<Reservation> bookings) {
                if (isFinishing() || isDestroyed()) return;
                java.util.Map<String, Reservation> stations = new java.util.LinkedHashMap<>();
                for (Reservation booking : bookings) stations.putIfAbsent(booking.stationId, booking);
                stationIds.clear(); stationIds.add("");
                List<String> labels = new ArrayList<>(); labels.add("All stations");
                for (Reservation booking : stations.values()) {
                    stationIds.add(booking.stationId);
                    labels.add(booking.stationLabel() + (booking.stationAddress.isEmpty() ? "" : " · " + booking.stationAddress));
                }
                stationField.setAdapter(new ArrayAdapter<>(SearchBookingActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                setBusy(false); renderResults(bookings);
            }
            @Override public void failure(int status, String error) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false); Toast.makeText(SearchBookingActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void doSearch() {
        String status = statuses[statusField.getSelectedItemPosition()];
        String station = stationIds.get(stationField.getSelectedItemPosition());

        setBusy(true);
        reservations.search(status, station, new ReservationRepository.Callback<List<Reservation>>() {
            @Override public void success(List<Reservation> list) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                renderResults(list);
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                if (status == 401) {
                    Toast.makeText(SearchBookingActivity.this, text, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(SearchBookingActivity.this, text, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void renderResults(List<Reservation> list) {
        listContainer.removeAllViews();
        if (list.isEmpty()) {
            TextView empty = text(getString(R.string.no_search_results), 14, MUTED, false);
            empty.setPadding(0, dp(20), 0, 0);
            return;
        }
        for (Reservation r : list) listContainer.addView(card(r));
    }

    private View card(Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), SolarStyle.BORDER);
        SolarStyle.card(card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);

        TextView rid = new TextView(this);
        rid.setText(r.stationLabel());
        rid.setTextSize(15); rid.setTextColor(INK);
        rid.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(rid);

        TextView meta = new TextView(this);
        meta.setText(r.stationAddress + "\n" + r.energyAmountKwh + " kWh · " + BookingText.trading(r.tradingType));
        meta.setTextSize(13); meta.setTextColor(MUTED);
        meta.setPadding(0, dp(4), 0, dp(4));
        card.addView(meta);

        TextView date = new TextView(this);
        date.setText(r.windowLabel() + "\nBooked for " + BookingText.date(r.reservationDate) + " (Sri Lanka)");
        date.setTextSize(13); date.setTextColor(INK);
        card.addView(date);

        TextView status = new TextView(this);
        status.setText(r.status);
        status.setTextSize(13); status.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        status.setTextColor(statusColor(r.status));
        status.setPadding(0, dp(6), 0, 0);
        card.addView(status);

        // Modify button (PENDING or APPROVED)
        if ("PENDING".equals(r.status) || "APPROVED".equals(r.status)) {
            Button modify = new Button(this);
            modify.setText(R.string.modify_booking);
            modify.setAllCaps(false); modify.setTextSize(13);
            modify.setTextColor(GREEN);
            SolarStyle.button(modify, false);
            modify.setMinHeight(dp(52));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
            mlp.topMargin = dp(10);
            card.addView(modify, mlp);
            modify.setOnClickListener(v -> {
                Intent i = new Intent(this, ModifyBookingActivity.class);
                i.putExtra("reservationId", r.id);
                BookingIntents.addDetails(i, r);
                i.putExtra("stationId", r.stationId);
                i.putExtra("slotId", r.slotId);
                i.putExtra("reservationDate", r.reservationDate);
                i.putExtra("energyAmountKwh", r.energyAmountKwh);
                i.putExtra("tradingType", r.tradingType);
                startActivity(i);
            });
        }
        return card;
    }

    private int statusColor(String status) {
        switch (status) {
            case "PENDING": return AMBER;
            case "APPROVED": return GREEN;
            case "CANCELLED": return RED;
            default: return INK;
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        for (Button b : actions) b.setEnabled(!value);
        statusField.setEnabled(!value);
        stationField.setEnabled(!value);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); SolarStyle.text(v, size, color, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        content.addView(v, lp);
        return v;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : GREEN);
        SolarStyle.button(b, primary);
        b.setMinHeight(dp(52));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        content.addView(b, lp);
        actions.add(b);
        b.setOnClickListener(v -> { if (!busy) action.run(); });
        return b;
    }

    private static String value(EditText input) { return input.getText().toString(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
