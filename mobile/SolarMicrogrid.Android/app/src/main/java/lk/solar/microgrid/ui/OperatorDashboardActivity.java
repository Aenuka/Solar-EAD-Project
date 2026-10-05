package lk.solar.microgrid.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.OperatorRepository;

public class OperatorDashboardActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;
    private OperatorRepository operators;
    private LinearLayout content;
    private SwipeRefreshLayout swipeRefresh;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        operators = ((SolarApplication) getApplication()).operators();
        if (!operators.signedIn()) {
            startActivity(new Intent(this, OperatorLoginActivity.class));
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SolarStyle.BACKGROUND);

        swipeRefresh = new SwipeRefreshLayout(this);
        swipeRefresh.setColorSchemeColors(GREEN);
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(26), dp(28), dp(26), dp(80)); // Extra padding for FAB
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        swipeRefresh.addView(scroll, new SwipeRefreshLayout.LayoutParams(-1, -1));

        root.addView(swipeRefresh, new FrameLayout.LayoutParams(-1, -1));

        Button fab = new Button(this);
        fab.setText("Scan QR");
        SolarStyle.button(fab, true);
        fab.setOnClickListener(v -> startActivity(new Intent(this, QrScannerActivity.class)));

        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(-2, dp(56));
        fabParams.gravity = Gravity.BOTTOM | Gravity.END;
        fabParams.setMargins(0, 0, dp(24), dp(24));
        root.addView(fab, fabParams);


        setContentView(root);
        root.requestApplyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (operators != null && operators.signedIn()) {
            loadDashboardData();
        }
    }

    private void loadDashboardData() {
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }
        content.removeAllViews();
        renderHeader();
        text("Loading dashboard...", 16, MUTED, false);

        operators.loadDashboard(new OperatorRepository.Callback<lk.solar.microgrid.data.OperatorDashboard>() {
            @Override
            public void success(lk.solar.microgrid.data.OperatorDashboard result) {
                if (isDestroyed() || isFinishing()) return;
                swipeRefresh.setRefreshing(false);
                renderDashboard(result);
            }

            @Override
            public void failure(int status, String message) {
                if (isDestroyed() || isFinishing()) return;
                swipeRefresh.setRefreshing(false);
                content.removeViewAt(content.getChildCount() - 1); // Remove loading text
                text(message != null ? message : "Unable to load dashboard. Please try again.", 16, SolarStyle.RED, false);
            }
        });
    }

    private void renderDashboard(lk.solar.microgrid.data.OperatorDashboard dashboard) {
        content.removeAllViews();

        renderHeader();

        // Stats
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.topMargin = dp(20);
        content.addView(statsLayout, statsParams);

        statsLayout.addView(statCard("Pending", String.valueOf(dashboard.pendingCount), "Awaiting approval", SolarStyle.AMBER));
        statsLayout.addView(statCard("Approved Future", String.valueOf(dashboard.approvedFutureCount), "Upcoming transfers", SolarStyle.BLUE));
        statsLayout.addView(statCard("Completed", String.valueOf(dashboard.completedCount), "Finished operations", GREEN));

        // Pending Reservations
        text("Pending Reservations", 20, INK, true).getLayoutParams().height = -2;
        ((LinearLayout.LayoutParams) content.getChildAt(content.getChildCount() - 1).getLayoutParams()).topMargin = dp(30);

        if (dashboard.pendingReservations.isEmpty()) {
            text("No pending reservations.", 14, MUTED, false);
        } else {
            for (lk.solar.microgrid.data.Reservation r : dashboard.pendingReservations) {
                renderReservationCard(r);
            }
        }

        button(R.string.view_completed_operations, false, () -> {
            startActivity(new Intent(this, CompletedOperationsActivity.class));
        });
    }

    private LinearLayout statCard(String title, String count, String label, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        SolarStyle.card(card);
        card.setPadding(dp(22), dp(20), dp(22), dp(20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        card.setLayoutParams(params);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        SolarStyle.text(titleView, 15, INK, true);
        labels.addView(titleView);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        SolarStyle.text(labelView, 14, MUTED, false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(4);
        labels.addView(labelView, labelParams);

        TextView countView = new TextView(this);
        countView.setText(count);
        SolarStyle.text(countView, 36, color, true);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-2, -2);
        countParams.leftMargin = dp(16);
        card.addView(countView, countParams);
        return card;
    }

    private void renderReservationCard(lk.solar.microgrid.data.Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int statusColor = "COMPLETED".equals(r.status) ? GREEN : "PENDING".equals(r.status) ? SolarStyle.AMBER : SolarStyle.BLUE;
        SolarStyle.card(card);
        card.setElevation(0);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        params.bottomMargin = dp(4);
        content.addView(card, params);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(headerRow, new LinearLayout.LayoutParams(-1, -2));

        TextView energyView = new TextView(this);
        energyView.setText(r.energyAmountKwh + " kWh");
        energyView.setTextSize(18); energyView.setTextColor(statusColor); energyView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        energyView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        headerRow.addView(energyView);

        TextView statusView = new TextView(this);
        statusView.setText(r.status);
        statusView.setTextSize(11);
        statusView.setTextColor(statusColor);
        statusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusView.setPadding(dp(12), dp(6), dp(12), dp(6));
        android.graphics.drawable.GradientDrawable statusBg = shape(Color.argb(30, Color.red(statusColor), Color.green(statusColor), Color.blue(statusColor)), 0);
        statusBg.setCornerRadius(dp(16));
        statusView.setBackground(statusBg);
        headerRow.addView(statusView);

        // Station ID
        TextView stationView = new TextView(this);
        String displayStation = (r.stationName != null && !r.stationName.isEmpty()) ? r.stationName : r.stationId;
        stationView.setText("Station: " + (displayStation != null && !displayStation.isEmpty() ? displayStation : "Unknown"));
        stationView.setTextSize(14); stationView.setTextColor(INK);
        LinearLayout.LayoutParams stationParams = new LinearLayout.LayoutParams(-1, -2);
        stationParams.topMargin = dp(16);
        card.addView(stationView, stationParams);

        // User NIC & Name
        TextView userView = new TextView(this);
        String displayUser = (r.prosumerName != null && !r.prosumerName.isEmpty()) ? r.prosumerName + " (" + r.prosumerNic + ")" : r.prosumerNic;
        userView.setText("User: " + (displayUser != null && !displayUser.isEmpty() ? displayUser : "Unknown"));
        userView.setTextSize(14); userView.setTextColor(INK);
        LinearLayout.LayoutParams userParams = new LinearLayout.LayoutParams(-1, -2);
        userParams.topMargin = dp(4);
        card.addView(userView, userParams);

        // Details (Type & Slots)
        if (r.tradingType != null && !r.tradingType.isEmpty()) {
            TextView detailsTxt = new TextView(this);
            detailsTxt.setText("Type: " + r.tradingType + " | Slots: " + r.allocationSlots);
            detailsTxt.setTextSize(14); detailsTxt.setTextColor(INK);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
            detailsParams.topMargin = dp(4);
            card.addView(detailsTxt, detailsParams);
        }

        // Formatted Date / Time
        String formattedDate = r.reservationDate;
        try {
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
            inFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inFormat.parse(r.reservationDate);
            java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault());
            formattedDate = outFormat.format(date);
        } catch (Exception e) {
            try {
                java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                java.util.Date date = inFormat.parse(r.reservationDate);
                java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault());
                formattedDate = outFormat.format(date);
            } catch (Exception ex) {}
        }

        TextView timeView = new TextView(this);
        String dateStr = "COMPLETED".equals(r.status) ?
            "Completed: " + r.source.optString("completedAt", r.source.optString("updatedAt", "Now"))
            : "Reserved: " + formattedDate;
        timeView.setText(dateStr);
        timeView.setTextSize(14); timeView.setTextColor(INK);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
        timeParams.topMargin = dp(4);
        card.addView(timeView, timeParams);

        // Subtitle: ID (small and muted now)
        TextView idView = new TextView(this);
        idView.setText("Ref: " + r.reservationId);
        idView.setTextSize(12); idView.setTextColor(MUTED);
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(-1, -2);
        idParams.topMargin = dp(8); idParams.bottomMargin = dp(8);
        card.addView(idView, idParams);

        if ("PENDING".equals(r.status)) {
            Button approveBtn = new Button(this);
            approveBtn.setText("Approve Booking");
            approveBtn.setTextSize(13);
            approveBtn.setAllCaps(false);
            approveBtn.setTextColor(Color.WHITE);
            SolarStyle.button(approveBtn, true);
            approveBtn.setMinHeight(dp(44));
            
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-1, -2);
            btnParams.topMargin = dp(12);
            card.addView(approveBtn, btnParams);
            
            approveBtn.setOnClickListener(v -> {
                approveBtn.setEnabled(false);
                approveBtn.setText("Approving...");
                operators.approveTransaction(r.id, new OperatorRepository.Callback<lk.solar.microgrid.data.Reservation>() {
                    @Override
                    public void success(lk.solar.microgrid.data.Reservation result) {
                        loadDashboardData();
                        android.widget.Toast.makeText(OperatorDashboardActivity.this, "Booking approved successfully!", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void failure(int status, String message) {
                        approveBtn.setEnabled(true);
                        approveBtn.setText("Approve Booking");
                        android.widget.Toast.makeText(OperatorDashboardActivity.this, message, android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }

    private void renderHeader() {
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.topMargin = dp(12); headerParams.bottomMargin = dp(12);
        content.addView(headerRow, headerParams);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        headerRow.addView(titleCol);

        TextView heading = new TextView(this);
        heading.setText(getString(R.string.operator_dashboard_title));
        heading.setTextSize(32); heading.setTextColor(INK); heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleCol.addView(heading);

        TextView name = new TextView(this);
        name.setText(operators.getFullName() != null ? operators.getFullName() : "Operator");
        SolarStyle.text(name, 16, MUTED, false);
        titleCol.addView(name);

        android.widget.ImageView profileIcon = new android.widget.ImageView(this);
        profileIcon.setImageResource(R.drawable.ic_account_avatar);
        profileIcon.setContentDescription("Operator Profile");
        profileIcon.setPadding(dp(8), dp(8), dp(8), dp(8));
        profileIcon.setOnClickListener(v -> startActivity(new Intent(this, OperatorProfileActivity.class)));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        headerRow.addView(profileIcon, iconParams);
    }

    private android.graphics.drawable.GradientDrawable shape(int fill, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); SolarStyle.text(view, size, color, bold);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
        content.addView(view, layout);
        return view;
    }

    private Button button(int label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(13); button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : GREEN);
        SolarStyle.button(button, primary);
        button.setMinHeight(dp(52));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(15);
        content.addView(button, layout);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
