package lk.solar.microgrid.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
    private int dashboardGeneration;

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String destination = intent.getStringExtra(SolarNavigation.DESTINATION);
        intent.removeExtra(SolarNavigation.DESTINATION);
        if ("COMPLETED".equals(destination)) SolarNavigation.open(this, SolarNavigation.Tab.COMPLETED);
        else if ("OPERATOR_ACCOUNT".equals(destination)) SolarNavigation.open(this, SolarNavigation.Tab.OPERATOR_ACCOUNT);
    }

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
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        swipeRefresh.addView(scroll, new SwipeRefreshLayout.LayoutParams(-1, -1));

        root.addView(swipeRefresh, new FrameLayout.LayoutParams(-1, -1));

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
        final int generation = ++dashboardGeneration;
        if (!swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }
        content.removeAllViews();
        renderHeader();
        text("Loading dashboard...", 16, MUTED, false);

        operators.loadDashboard(new OperatorRepository.Callback<lk.solar.microgrid.data.OperatorDashboard>() {
            @Override
            public void success(lk.solar.microgrid.data.OperatorDashboard result) {
                if (isDestroyed() || isFinishing() || generation != dashboardGeneration) return;
                swipeRefresh.setRefreshing(false);
                renderDashboard(result);
            }

            @Override
            public void failure(int status, String message) {
                if (isDestroyed() || isFinishing() || generation != dashboardGeneration) return;
                swipeRefresh.setRefreshing(false);
                content.removeViewAt(content.getChildCount() - 1); // Remove loading text
                TextView error = text(message != null ? message : "Unable to load dashboard. Please try again.", 16, SolarStyle.RED, false);
                SolarStyle.notice(error, true);
                button(R.string.refresh, false, OperatorDashboardActivity.this::loadDashboardData);
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

        // Subtitle: ID
        TextView idView = new TextView(this);
        idView.setText("Ref: " + r.reservationId);
        idView.setTextSize(14); idView.setTextColor(INK); idView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(-1, -2);
        idParams.topMargin = dp(16);
        card.addView(idView, idParams);

        TextView detailsView = new TextView(this);
        String dateStr = "COMPLETED".equals(r.status) ?
            "Completed: " + r.source.optString("completedAt", r.source.optString("updatedAt", "Now"))
            : "Reserved: " + r.reservationDate;

        detailsView.setText(dateStr);
        detailsView.setTextSize(13); detailsView.setTextColor(MUTED);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
        detailsParams.topMargin = dp(8);
        card.addView(detailsView, detailsParams);
    }

    private void renderHeader() {
        SolarStyle.hero(content, "Overview", "Welcome back, " + (operators.getFullName() != null ? operators.getFullName() : "Operator") + ".");
        Button scan = button(R.string.scan_transaction_qr, true, () -> startActivity(new Intent(this, QrScannerActivity.class)));
        ((LinearLayout.LayoutParams) scan.getLayoutParams()).topMargin = 0;
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
