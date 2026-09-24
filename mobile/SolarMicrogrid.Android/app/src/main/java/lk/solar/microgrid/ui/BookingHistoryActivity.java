package lk.solar.microgrid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
 * Shows the prosumer's reservation history (live from the API).
 * Author: Sajith
 */
public final class BookingHistoryActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77),
            INK = Color.rgb(23, 61, 50),
            MUTED = Color.rgb(107, 123, 117),
            AMBER = Color.rgb(191, 132, 0),
            RED = Color.rgb(155, 66, 44);

    private ReservationRepository reservations;
    private LinearLayout content, listContainer;
    private ProgressBar progress;
    private Button refreshBtn;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        buildUi();
        load();
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
        TextView heading = text(getString(R.string.my_bookings), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(24);
        text("All your reservations, newest first.", 14, MUTED, false);

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
        reservations.history(new ReservationRepository.Callback<List<Reservation>>() {
            @Override public void success(List<Reservation> list) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                renderList(list);
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                if (status == 401) { Toast.makeText(BookingHistoryActivity.this, text, Toast.LENGTH_LONG).show(); finish(); }
                else Toast.makeText(BookingHistoryActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderList(List<Reservation> list) {
        listContainer.removeAllViews();
        if (list.isEmpty()) {
            TextView empty = text(getString(R.string.no_bookings), 14, MUTED, false);
            empty.setPadding(0, dp(20), 0, 0);
            return;
        }
        for (Reservation r : list) {
            listContainer.addView(card(r));
        }
    }

    private View card(Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(10));
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

        // Modify button for PENDING or APPROVED bookings
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
                android.content.Intent i = new android.content.Intent(this, ModifyBookingActivity.class);
                i.putExtra("reservationId", r.id);
                i.putExtra("stationId", r.stationId);
                i.putExtra("slotId", r.slotId);
                i.putExtra("reservationDate", r.reservationDate);
                i.putExtra("energyAmountKwh", r.energyAmountKwh);
                i.putExtra("tradingType", r.tradingType);
                startActivity(i);
            });
        }

        // Cancel button for PENDING or APPROVED bookings
        if ("PENDING".equals(r.status) || "APPROVED".equals(r.status)) {
            Button cancel = new Button(this);
            cancel.setText(getString(R.string.cancel_booking));
            cancel.setAllCaps(false); cancel.setTextSize(12);
            cancel.setTextColor(RED);
            cancel.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(250, 235, 230)));
            cancel.setMinHeight(dp(40));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
            clp.topMargin = dp(10);
            card.addView(cancel, clp);
            cancel.setOnClickListener(v -> confirmCancel(r));
        }
        return card;
    }

    private void confirmCancel(Reservation r) {
        EditText reason = new EditText(this);
        reason.setHint(getString(R.string.cancel_reason));
        reason.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_cancel))
                .setView(reason)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.cancel_booking, (d, w) -> {
                    String reasonText = reason.getText().toString().trim();
                    doCancel(r.id, reasonText);
                }).show();
    }

    private void doCancel(String id, String reason) {
        reservations.cancel(id, reason, new ReservationRepository.Callback<Reservation>() {
            @Override public void success(Reservation updated) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(BookingHistoryActivity.this, R.string.booking_cancelled, Toast.LENGTH_SHORT).show();
                load();
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(BookingHistoryActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int statusColor(String status) {
        switch (status) {
            case "PENDING": return AMBER;
            case "APPROVED": return GREEN;
            case "CANCELLED": return RED;
            default: return INK;
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        refreshBtn.setEnabled(!loading);
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
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}