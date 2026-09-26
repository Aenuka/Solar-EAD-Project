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
import android.widget.ScrollView;
import android.widget.TextView;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.OperatorRepository;

public class OperatorDashboardActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);
    private OperatorRepository operators;
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        operators = ((SolarApplication) getApplication()).operators();
        if (!operators.signedIn()) {
            startActivity(new Intent(this, OperatorLoginActivity.class));
            finish();
            return;
        }
        
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 243));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(26), dp(28), dp(26), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (operators != null && operators.signedIn()) {
            loadDashboardData();
        }
    }

    private void loadDashboardData() {
        content.removeAllViews();
        
        TextView heading = text(getString(R.string.operator_dashboard_title), 32, INK, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(12);
        
        text(operators.getFullName() != null ? operators.getFullName() : "Operator", 24, INK, true);
        
        button(R.string.scan_transaction_qr, true, () -> {
            startActivity(new Intent(this, QrScannerActivity.class));
        });
        
        Button refreshBtn = button(R.string.refresh, false, this::loadDashboardData);
        refreshBtn.setText("Refresh");
        
        text("Loading dashboard...", 16, MUTED, false);

        operators.loadDashboard(new OperatorRepository.Callback<lk.solar.microgrid.data.OperatorDashboard>() {
            @Override
            public void success(lk.solar.microgrid.data.OperatorDashboard result) {
                if (isDestroyed() || isFinishing()) return;
                renderDashboard(result);
            }

            @Override
            public void failure(int status, String message) {
                if (isDestroyed() || isFinishing()) return;
                content.removeViewAt(content.getChildCount() - 1); // Remove loading text
                text(message != null ? message : "Unable to load dashboard. Please try again.", 16, Color.RED, false);
            }
        });
    }

    private void renderDashboard(lk.solar.microgrid.data.OperatorDashboard dashboard) {
        content.removeAllViews();
        
        TextView heading = text(getString(R.string.operator_dashboard_title), 32, INK, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(12);
        
        text(operators.getFullName() != null ? operators.getFullName() : "Operator", 24, INK, true);
        
        // Stats
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.topMargin = dp(20);
        content.addView(statsLayout, statsParams);
        
        statsLayout.addView(statCard("Pending", dashboard.pendingCount, Color.rgb(255, 152, 0)));
        statsLayout.addView(statCard("Approved\nFuture", dashboard.approvedFutureCount, Color.rgb(33, 150, 243)));
        statsLayout.addView(statCard("Completed", dashboard.completedCount, GREEN));

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

        // Completed Reservations
        text("Completed Operations", 20, INK, true).getLayoutParams().height = -2;
        ((LinearLayout.LayoutParams) content.getChildAt(content.getChildCount() - 1).getLayoutParams()).topMargin = dp(30);
        
        if (dashboard.recentCompletedReservations.isEmpty()) {
            text("No completed transactions yet.", 14, MUTED, false);
        } else {
            for (lk.solar.microgrid.data.Reservation r : dashboard.recentCompletedReservations) {
                renderReservationCard(r);
            }
        }

        button(R.string.scan_transaction_qr, true, () -> {
            startActivity(new Intent(this, QrScannerActivity.class));
        });
        
        Button refreshBtn = button(R.string.refresh, false, this::loadDashboardData);
        refreshBtn.setText("Refresh");

        button(R.string.sign_out, false, () -> {
            operators.logout();
            startActivity(new Intent(this, OperatorLoginActivity.class));
            finish();
        });
    }

    private LinearLayout statCard(String title, int count, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1.0f);
        params.rightMargin = dp(8);
        card.setLayoutParams(params);
        
        TextView titleView = new TextView(this);
        titleView.setText(title); titleView.setTextSize(12); titleView.setTextColor(MUTED);
        card.addView(titleView);
        
        TextView countView = new TextView(this);
        countView.setText(String.valueOf(count)); countView.setTextSize(24); countView.setTextColor(color);
        countView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(countView);
        return card;
    }

    private void renderReservationCard(lk.solar.microgrid.data.Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        content.addView(card, params);

        TextView idView = new TextView(this);
        idView.setText(r.reservationId); idView.setTextSize(16); idView.setTextColor(INK); idView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(idView);

        TextView detailsView = new TextView(this);
        detailsView.setText(r.reservationDate + "\n" + r.energyAmountKwh + " kWh");
        detailsView.setTextSize(14); detailsView.setTextColor(MUTED);
        card.addView(detailsView);

        TextView statusView = new TextView(this);
        statusView.setText(r.status);
        statusView.setTextSize(14); statusView.setTextColor("COMPLETED".equals(r.status) ? GREEN : "PENDING".equals(r.status) ? Color.rgb(255, 152, 0) : MUTED);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(statusView);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
        content.addView(view, layout);
        return view;
    }

    private Button button(int label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(13); button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : GREEN);
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(234, 240, 227)));
        button.setMinHeight(dp(50));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(15);
        content.addView(button, layout);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
