package lk.solar.microgrid.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
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

import java.util.ArrayList;
import java.util.List;

import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Search reservations by status and station ID.
 * Author: Sajith
 */
public final class SearchBookingActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77),
            INK = Color.rgb(23, 61, 50),
            MUTED = Color.rgb(107, 123, 117),
            AMBER = Color.rgb(191, 132, 0),
            RED = Color.rgb(155, 66, 44);

    private ReservationRepository reservations;
    private LinearLayout content, listContainer;
    private ProgressBar progress;
    private EditText statusField, stationField;
    private Button searchBtn;
    private final List<Button> actions = new ArrayList<>();

    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 243));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView brand = text(getString(R.string.brand), 12, GREEN, true);
        brand.setLetterSpacing(0.13f);
        TextView heading = text(getString(R.string.search_booking), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(24);
        text(getString(R.string.search_criteria), 14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        // Status field
        TextView cap1 = text(getString(R.string.status_label), 12, INK, true);
        ((LinearLayout.LayoutParams) cap1.getLayoutParams()).topMargin = dp(18);
        statusField = new EditText(this);
        statusField.setTextSize(15); statusField.setTextColor(INK); statusField.setHintTextColor(MUTED);
        statusField.setHint(R.string.search_hint_status);
        statusField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        statusField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        GradientDrawable bg1 = new GradientDrawable();
        bg1.setColor(Color.WHITE); bg1.setCornerRadius(dp(8));
        bg1.setStroke(dp(1), Color.rgb(213, 224, 214));
        statusField.setBackground(bg1);
        statusField.setPadding(dp(13), dp(12), dp(13), dp(12));
        statusField.setMinimumHeight(dp(50));
        content.addView(statusField, new LinearLayout.LayoutParams(-1, -2));

        // Station field
        TextView cap2 = text(getString(R.string.station_id_label), 12, INK, true);
        ((LinearLayout.LayoutParams) cap2.getLayoutParams()).topMargin = dp(18);
        stationField = new EditText(this);
        stationField.setTextSize(15); stationField.setTextColor(INK); stationField.setHintTextColor(MUTED);
        stationField.setHint(R.string.search_hint_station);
        stationField.setInputType(InputType.TYPE_CLASS_TEXT);
        stationField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        GradientDrawable bg2 = new GradientDrawable();
        bg2.setColor(Color.WHITE); bg2.setCornerRadius(dp(8));
        bg2.setStroke(dp(1), Color.rgb(213, 224, 214));
        stationField.setBackground(bg2);
        stationField.setPadding(dp(13), dp(12), dp(13), dp(12));
        stationField.setMinimumHeight(dp(50));
        content.addView(stationField, new LinearLayout.LayoutParams(-1, -2));

        // Search button
        searchBtn = button(getString(R.string.search_button), true, this::doSearch);

        // Clear button
        button(getString(R.string.clear_button), false, () -> {
            statusField.setText("");
            stationField.setText("");
            listContainer.removeAllViews();
        });

        // Results
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void doSearch() {
        String status = value(statusField).trim().toUpperCase();
        String station = value(stationField).trim();
        if (status.isEmpty()) status = null;
        if (station.isEmpty()) station = null;

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
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), Color.rgb(213, 224, 214));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);

        TextView rid = new TextView(this);
        rid.setText(r.reservationId.isEmpty() ? r.id : r.reservationId);
        rid.setTextSize(15); rid.setTextColor(INK);
        rid.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(rid);

        TextView meta = new TextView(this);
        meta.setText(r.stationId + " · " + r.energyAmountKwh + " kWh · " + r.tradingType);
        meta.setTextSize(13); meta.setTextColor(MUTED);
        meta.setPadding(0, dp(4), 0, dp(4));
        card.addView(meta);

        TextView date = new TextView(this);
        date.setText("When: " + r.reservationDate);
        date.setTextSize(13); date.setTextColor(INK);
        card.addView(date);

        TextView status = new TextView(this);
        status.setText(r.status);
        status.setTextSize(12); status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setTextColor(statusColor(r.status));
        status.setPadding(0, dp(6), 0, 0);
        card.addView(status);

        // Modify button (PENDING or APPROVED)
        if ("PENDING".equals(r.status) || "APPROVED".equals(r.status)) {
            Button modify = new Button(this);
            modify.setText(R.string.modify_booking);
            modify.setAllCaps(false); modify.setTextSize(12);
            modify.setTextColor(GREEN);
            modify.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(234, 240, 227)));
            modify.setMinHeight(dp(40));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
            mlp.topMargin = dp(10);
            card.addView(modify, mlp);
            modify.setOnClickListener(v -> {
                Intent i = new Intent(this, ModifyBookingActivity.class);
                i.putExtra("reservationId", r.id);
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
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        content.addView(v, lp);
        return v;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : GREEN);
        b.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(234, 240, 227)));
        b.setMinHeight(dp(48));
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