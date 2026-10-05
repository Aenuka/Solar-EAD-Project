package lk.solar.microgrid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import lk.solar.microgrid.R;
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
        content.setPadding(dp(24), dp(24), dp(24), dp(36));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));


        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (operators != null && operators.signedIn()) {
            loadOperations();
        }
    }

    private void loadOperations() {
        content.removeAllViews();

        // Top Navigation / Back button
        TextView backBtn = new TextView(this);
        backBtn.setText("← Back to Dashboard");
        backBtn.setTextSize(14); backBtn.setTextColor(GREEN); backBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        backBtn.setPadding(0, dp(10), 0, dp(20));
        backBtn.setClickable(true);
        backBtn.setOnClickListener(v -> finish());
        content.addView(backBtn);

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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        SolarStyle.card(card);
        card.setElevation(0);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setClickable(true);
        card.setOnClickListener(v -> showDetailsDialog(r));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        params.bottomMargin = dp(6);
        content.addView(card, params);

        android.view.animation.TranslateAnimation anim = new android.view.animation.TranslateAnimation(
            android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0f
        );
        anim.setDuration(400);
        anim.setStartOffset(index * 50L);
        anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
        card.startAnimation(anim);

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
        String displayStation = (r.stationName != null && !r.stationName.isEmpty()) ? r.stationName : r.stationId;
        if (displayStation != null && displayStation.length() > 25) {
            displayStation = displayStation.substring(0, 25) + "...";
        }
        stationView.setText("Station: " + (displayStation != null ? displayStation : "Unknown"));
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

        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            String cleanDate = completedDate.split("\\.")[0]; // Remove milliseconds
            java.util.Date d = in.parse(cleanDate);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
            completedDate = out.format(d);
        } catch (Exception e) {
            completedDate = completedDate.replace("T", " ").replace("Z", "");
            if (completedDate.length() > 19) {
                completedDate = completedDate.substring(0, 19);
            }
        }

        detailsView.setText("Completed " + completedDate);
        detailsView.setTextSize(13); detailsView.setTextColor(MUTED);
        LinearLayout.LayoutParams dpL = new LinearLayout.LayoutParams(-1, -2);
        dpL.topMargin = dp(16);
        card.addView(detailsView, dpL);
    }

    private void showDetailsDialog(Reservation r) {
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
        row1.addView(detailCell("Type", r.tradingType, false));

        // Row 2: Status & Prosumer
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(16), 0, 0);
        layout.addView(row2, new LinearLayout.LayoutParams(-1, -2));

        String displayUser = (r.prosumerName != null && !r.prosumerName.isEmpty()) ? r.prosumerName : r.prosumerNic;
        row2.addView(detailCell("Status", r.status, true));
        row2.addView(detailCell("Prosumer", displayUser, false));

        // Station
        String displayStation = (r.stationName != null && !r.stationName.isEmpty()) ? r.stationName : r.stationId;
        if (displayStation != null && displayStation.length() > 25) displayStation = displayStation.substring(0, 25) + "...";
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
        if (iso == null || iso.isEmpty() || iso.equals("Unknown")) return "N/A";
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            String cleanDate = iso.split("\\.")[0];
            java.util.Date d = in.parse(cleanDate);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("MMM dd, yyyy\nhh:mm a");
            return out.format(d);
        } catch (Exception e) {
            String fallback = iso.replace("T", " ").replace("Z", "");
            if (fallback.length() > 19) fallback = fallback.substring(0, 19);
            return fallback;
        }
    }

    private android.graphics.drawable.GradientDrawable shape(int fill, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
