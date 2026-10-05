package lk.solar.microgrid.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Shows all pending reservations from the API.
 * Author: Sajith
 */
public final class PendingBookingsActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN,
            INK = SolarStyle.INK,
            MUTED = SolarStyle.MUTED,
            AMBER = SolarStyle.AMBER;

    private ReservationRepository reservations;
    private LinearLayout content, listContainer;
    private ProgressBar progress;
    private Button refreshBtn;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        load();
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

        TextView heading = text(getString(R.string.pending_bookings), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(8);
        text("Reservations awaiting operator review.", 14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        refreshBtn = button(getString(R.string.refresh), true, this::load);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void load() {
        setLoading(true);
        reservations.pending(new ReservationRepository.Callback<List<Reservation>>() {
            @Override public void success(List<Reservation> list) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                renderList(list);
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                if (status == 401) { Toast.makeText(PendingBookingsActivity.this, text, Toast.LENGTH_LONG).show(); finish(); }
                else Toast.makeText(PendingBookingsActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderList(List<Reservation> list) {
        listContainer.removeAllViews();
        if (list.isEmpty()) {
            TextView empty = text(getString(R.string.no_pending), 14, MUTED, false);
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
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), SolarStyle.BORDER);
        SolarStyle.card(card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);

        TextView rid = new TextView(this);
        rid.setText(r.reservationId.isEmpty() ? r.id : r.reservationId);
        rid.setTextSize(15); rid.setTextColor(INK);
        rid.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(rid);

        TextView meta = new TextView(this);
        meta.setText(r.stationId + " · " + r.energyAmountKwh + " kWh · " + r.tradingType);
        meta.setTextSize(13); meta.setTextColor(MUTED);
        meta.setPadding(0, dp(4), 0, dp(4));
        card.addView(meta);

        TextView nic = new TextView(this);
        nic.setText("Prosumer: " + r.prosumerNic);
        nic.setTextSize(13); nic.setTextColor(MUTED);
        card.addView(nic);

        TextView date = new TextView(this);
        date.setText("When: " + r.reservationDate);
        date.setTextSize(13); date.setTextColor(INK);
        date.setPadding(0, dp(6), 0, 0);
        card.addView(date);

        TextView status = new TextView(this);
        status.setText(r.status);
        status.setTextSize(13); status.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        status.setTextColor(AMBER);
        status.setPadding(0, dp(6), 0, 0);
        card.addView(status);

        return card;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        refreshBtn.setEnabled(!loading);
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
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
