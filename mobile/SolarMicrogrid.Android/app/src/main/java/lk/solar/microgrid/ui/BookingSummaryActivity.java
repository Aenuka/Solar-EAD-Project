package lk.solar.microgrid.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import lk.solar.microgrid.R;

/**
 * Read-only summary of a single reservation.
 * Author: Sajith
 */
public final class BookingSummaryActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77),
            INK = Color.rgb(23, 61, 50),
            MUTED = Color.rgb(107, 123, 117),
            AMBER = Color.rgb(191, 132, 0),
            RED = Color.rgb(155, 66, 44);

    private LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        TextView heading = text(getString(R.string.booking_summary_title), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(24);
        text("Reservation details:", 14, MUTED, false);

        String reservationCode = getIntent().getStringExtra("reservationCode");
        String stationId = getIntent().getStringExtra("stationId");
        String slotId = getStringExtraSafe("slotId");
        String reservationDate = getStringExtraSafe("reservationDate");
        double energy = getIntent().getDoubleExtra("energyAmountKwh", 0);
        String tradingType = getStringExtraSafe("tradingType");
        String status = getStringExtraSafe("status");
        String token = getStringExtraSafe("transactionToken");
        String reason = getStringExtraSafe("cancellationReason");

        // Card with details
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(213, 224, 214));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        content.addView(card, lp);

        //addRow(card, getString(R.string.reservation_id_label_or_default()), reservationCode == null ? "" : reservationCode);
        addRow(card, "Reservation ID", reservationCode == null ? "" : reservationCode);
        addRow(card, getString(R.string.station_id_label), stationId == null ? "" : stationId);
        addRow(card, getString(R.string.slot_id_label), slotId == null ? "" : slotId);
        addRow(card, getString(R.string.reservation_date_label), reservationDate == null ? "" : reservationDate);
        addRow(card, getString(R.string.energy_amount_label), energy + " kWh");
        addRow(card, getString(R.string.trading_type_label), tradingType == null ? "" : tradingType);
        addRow(card, getString(R.string.status_label), status == null ? "" : status);

        if (token != null && !token.isEmpty()) {
            addRow(card, "Transaction Token", token);
        }
        if (reason != null && !reason.isEmpty()) {
            addRow(card, "Cancellation Reason", reason);
        }



        // Close button
        Button close = new Button(this);
        close.setText(R.string.close_button);
        close.setTextSize(13); close.setAllCaps(false);
        close.setTextColor(Color.WHITE);
        close.setBackgroundTintList(ColorStateList.valueOf(MUTED));
        close.setMinHeight(dp(48));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = dp(10);
        content.addView(close, blp);
        close.setOnClickListener(v -> finish());
    }

    private String getStringExtraSafe(String key) {
        String v = getIntent().getStringExtra(key);
        return v == null ? "" : v;
    }

    private void addRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        parent.addView(row, rlp);

        TextView l = new TextView(this);
        l.setText(label + ": ");
        l.setTextSize(13); l.setTextColor(MUTED);
        l.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(13); v.setTextColor(INK);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(v, vlp);
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

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}