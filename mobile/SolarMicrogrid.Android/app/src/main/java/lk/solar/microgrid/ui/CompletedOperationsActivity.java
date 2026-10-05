/*
 * File: CompletedOperationsActivity.java
 * Author: Lakshman K A P C
 * Description: Android activity displaying a history of completed energy transfers.
 */
package lk.solar.microgrid.ui;

import lk.solar.microgrid.data.BookingText;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.OperatorRepository;
import lk.solar.microgrid.data.Reservation;
import java.util.List;

public class CompletedOperationsActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;
    private OperatorRepository operators;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the activity, layout, and verify operator authentication
        super.onCreate(savedInstanceState);
        operators = ((SolarApplication) getApplication()).operators();
        if (!operators.signedIn()) {
            finish();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));


        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    @Override
    protected void onResume() {
        // Reload completed operations list when the activity comes to the foreground
        super.onResume();
        if (operators != null && operators.signedIn()) {
            loadOperations();
        }
    }

    private void loadOperations() {
        // Fetch completed operations from the server and render the UI
        content.removeAllViews();



        TextView eyebrow = new TextView(this);
        eyebrow.setText("HISTORY");
        eyebrow.setTextSize(12); eyebrow.setTextColor(MUTED); eyebrow.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        content.addView(eyebrow);

        TextView heading = new TextView(this);
        heading.setText("Completed Operations");
        heading.setTextSize(32); heading.setTextColor(INK); heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams headingLayout = new LinearLayout.LayoutParams(-1, -2);
        headingLayout.topMargin = dp(4); headingLayout.bottomMargin = dp(6);
        content.addView(heading, headingLayout);

        TextView sub = new TextView(this);
        sub.setText("Previously finalized energy transfers.");
        sub.setTextSize(14); sub.setTextColor(MUTED);
        LinearLayout.LayoutParams subLayout = new LinearLayout.LayoutParams(-1, -2);
        subLayout.bottomMargin = dp(28);
        content.addView(sub, subLayout);

        TextView loading = new TextView(this);
        loading.setText("Loading completed operations...");
        loading.setTextSize(16); loading.setTextColor(MUTED);
        content.addView(loading);

        operators.searchCompletedOperations(new OperatorRepository.Callback<List<Reservation>>() {
            @Override
            public void success(List<Reservation> result) {
                if (isDestroyed() || isFinishing()) return;
                content.removeView(loading);

                if (result.isEmpty()) {
                    TextView empty = new TextView(CompletedOperationsActivity.this);
                    empty.setText("No completed operations yet.");
                    empty.setTextSize(14); empty.setTextColor(MUTED);
                    content.addView(empty);
                } else {
                    int index = 0;
                    for (Reservation r : result) {
                        renderCard(r, index++);
                    }
                }
            }

            @Override
            public void failure(int status, String message) {
                if (isDestroyed() || isFinishing()) return;
                content.removeView(loading);

                TextView error = new TextView(CompletedOperationsActivity.this);
                error.setText(message != null ? message : "Unable to load completed operations. Please try again.");
                error.setTextSize(16); error.setTextColor(SolarStyle.RED);
                content.addView(error);
            }
        });
    }

    private void renderCard(Reservation r, int index) {
        // Render a summary card for a completed operation
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        SolarStyle.interactiveCard(card);
        card.setElevation(0);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setClickable(true);
        card.setOnClickListener(v -> showDetailsDialog(r));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        params.bottomMargin = dp(6);
        content.addView(card, params);

        // Header Row: Energy amount and Status
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(headerRow, new LinearLayout.LayoutParams(-1, -2));

        TextView energyView = new TextView(this);
        energyView.setText(r.energyAmountKwh + " kWh");
        energyView.setTextSize(18); energyView.setTextColor(GREEN); energyView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        energyView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        headerRow.addView(energyView);

        TextView statusView = new TextView(this);
        statusView.setText("✓ COMPLETED");
        statusView.setTextSize(11);
        statusView.setTextColor(GREEN);
        statusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusView.setPadding(dp(12), dp(6), dp(12), dp(6));
        android.graphics.drawable.GradientDrawable statusBg = shape(Color.rgb(234, 247, 239), 0);
        statusBg.setCornerRadius(dp(16));
        statusView.setBackground(statusBg);
        headerRow.addView(statusView);

        // Subtitle: ID
        TextView idView = new TextView(this);
        idView.setText("Ref: " + r.reservationId);
        idView.setTextSize(14); idView.setTextColor(INK); idView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(-1, -2);
        idParams.topMargin = dp(16);
        card.addView(idView, idParams);

        // Subtitle: Station
        TextView stationView = new TextView(this);
        String shortStation = r.stationLabel();
        stationView.setText("Station: " + shortStation);
        stationView.setTextSize(14); stationView.setTextColor(MUTED);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4);
        card.addView(stationView, sp);

        // Subtitle: User
        TextView userView = new TextView(this);
        String displayUser = (r.prosumerName != null && !r.prosumerName.isEmpty()) ? r.prosumerName + " (" + r.prosumerNic + ")" : r.prosumerNic;
        userView.setText("User: " + (displayUser != null ? displayUser : "Unknown"));
        userView.setTextSize(14); userView.setTextColor(MUTED);
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, -2);
        up.topMargin = dp(4);
        card.addView(userView, up);

        // Date row
        TextView detailsView = new TextView(this);
        String completedDate = r.source.optString("completedAt", "");
        if (completedDate.isEmpty()) completedDate = r.source.optString("updatedAt", "");

        detailsView.setText(r.windowLabel() + "\nCompleted " + BookingText.date(completedDate) + " (Sri Lanka)");
        detailsView.setTextSize(13); detailsView.setTextColor(MUTED);
        LinearLayout.LayoutParams dpL = new LinearLayout.LayoutParams(-1, -2);
        dpL.topMargin = dp(16);
        card.addView(detailsView, dpL);
    }

    private void showDetailsDialog(Reservation r) {
        // Show a dialog with detailed information about the selected operation
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));
        scroll.addView(layout);

        // Header
        TextView title = new TextView(this);
        title.setText("Transfer Details");
        title.setTextSize(22);
        title.setTextColor(INK);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ref: " + r.reservationId);
        subtitle.setTextSize(13);
        subtitle.setTextColor(MUTED);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.bottomMargin = dp(24);
        layout.addView(subtitle, subLp);

        // Row 1: Energy & Trading Type
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(row1, new LinearLayout.LayoutParams(-1, -2));

        row1.addView(detailCell("Energy", r.energyAmountKwh + " kWh", true));
        row1.addView(detailCell("Type", BookingText.trading(r.tradingType), false));

        // Row 2: Status & Prosumer
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(16), 0, 0);
        layout.addView(row2, new LinearLayout.LayoutParams(-1, -2));

        String displayUser = (r.prosumerName != null && !r.prosumerName.isEmpty()) ? r.prosumerName : r.prosumerNic;
        row2.addView(detailCell("Status", r.status, true));
        row2.addView(detailCell("Prosumer", r.prosumerLabel(), false));

        // Station
        String shortStation = r.stationLabel();
        LinearLayout rowStation = new LinearLayout(this);
        rowStation.setOrientation(LinearLayout.HORIZONTAL);
        rowStation.setPadding(0, dp(16), 0, 0);
        layout.addView(rowStation, new LinearLayout.LayoutParams(-1, -2));
        rowStation.addView(detailCell("Station", displayStation, false));

        // Divider
        android.view.View div = new android.view.View(this);
        div.setBackgroundColor(Color.rgb(235, 235, 235));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, dp(1));
        divLp.setMargins(0, dp(20), 0, dp(20));
        layout.addView(div, divLp);

        // Dates
        String completedAt = r.source.optString("completedAt", r.source.optString("updatedAt", ""));
        LinearLayout rowDates = new LinearLayout(this);
        rowDates.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(rowDates, new LinearLayout.LayoutParams(-1, -2));

        rowDates.addView(detailCell("Booked", formatDate(r.reservationDate), false));
        rowDates.addView(detailCell("Completed", formatDate(completedAt), false));

        new AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private LinearLayout detailCell(String label, String value, boolean isPrimary) {
        // Create a layout cell for displaying a label and its value
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(12);
        l.setTextColor(MUTED);
        cell.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(15);
        v.setTextColor(isPrimary ? GREEN : INK);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        cell.addView(v);

        return cell;
    }

    private String formatDate(String iso) { 
        // Format an ISO date string for display
        return BookingText.date(iso); 
    }

    private android.graphics.drawable.GradientDrawable shape(int fill, int stroke) {
        // Create a drawable shape with specified fill and stroke
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) { 
        // Convert dp units to pixels
        return Math.round(value * getResources().getDisplayMetrics().density); 
    }
}
