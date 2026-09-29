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

public class OperatorDashboardActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);
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
        root.setBackgroundColor(Color.rgb(245, 247, 243));

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
        fab.setTextColor(Color.WHITE);
        fab.setAllCaps(false);
        fab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fab.setElevation(dp(6));
        android.graphics.drawable.GradientDrawable fabShape = new android.graphics.drawable.GradientDrawable();
        fabShape.setCornerRadius(dp(28));
        fabShape.setColor(GREEN);
        fab.setBackground(fabShape);
        fab.setPadding(dp(20), 0, dp(20), 0);
        fab.setOnClickListener(v -> startActivity(new Intent(this, QrScannerActivity.class)));
        
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(-2, dp(56));
        fabParams.gravity = Gravity.BOTTOM | Gravity.END;
        fabParams.setMargins(0, 0, dp(24), dp(24));
        root.addView(fab, fabParams);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        
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
                text(message != null ? message : "Unable to load dashboard. Please try again.", 16, Color.RED, false);
            }
        });
    }

    private void renderDashboard(lk.solar.microgrid.data.OperatorDashboard dashboard) {
        content.removeAllViews();
        
        renderHeader();
        
        // Stats
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.topMargin = dp(20);
        content.addView(statsLayout, statsParams);
        
        statsLayout.addView(statCard("Pending", String.valueOf(dashboard.pendingCount), "Awaiting approval", Color.rgb(255, 152, 0)));
        statsLayout.addView(statCard("Approved Future", String.valueOf(dashboard.approvedFutureCount), "Upcoming transfers", Color.rgb(33, 150, 243)));
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
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(Color.WHITE, 0));
        card.setElevation(dp(2));
        card.setPadding(dp(12), dp(16), dp(12), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1.0f);
        params.rightMargin = dp(8);
        card.setLayoutParams(params);
        
        TextView titleView = new TextView(this);
        titleView.setText(title); titleView.setTextSize(12); titleView.setTextColor(INK);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(titleView);
        
        TextView countView = new TextView(this);
        countView.setText(count); countView.setTextSize(28); countView.setTextColor(color);
        countView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(countView);

        TextView labelView = new TextView(this);
        labelView.setText(label); labelView.setTextSize(10); labelView.setTextColor(MUTED);
        card.addView(labelView);
        
        return card;
    }

    private void renderReservationCard(lk.solar.microgrid.data.Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int statusColor = "COMPLETED".equals(r.status) ? GREEN : "PENDING".equals(r.status) ? Color.rgb(255, 152, 0) : Color.rgb(33, 150, 243);
        android.graphics.drawable.GradientDrawable bg = shape(Color.WHITE, 0);
        bg.setStroke(dp(2), statusColor);
        card.setBackground(bg);
        card.setElevation(dp(3));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        params.bottomMargin = dp(4);
        content.addView(card, params);

        TextView idView = new TextView(this);
        idView.setText(r.reservationId); idView.setTextSize(16); idView.setTextColor(INK); idView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(idView);

        TextView detailsView = new TextView(this);
        String dateStr = "COMPLETED".equals(r.status) ? 
            "Completed: " + r.source.optString("completedAt", r.source.optString("updatedAt", "Now")) 
            : r.reservationDate;
            
        detailsView.setText(dateStr + "\n" + r.energyAmountKwh + " kWh");
        detailsView.setTextSize(14); detailsView.setTextColor(MUTED);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
        detailsParams.topMargin = dp(4); detailsParams.bottomMargin = dp(8);
        card.addView(detailsView, detailsParams);

        TextView statusView = new TextView(this);
        statusView.setText(r.status);
        statusView.setTextSize(12); 
        statusView.setTextColor("COMPLETED".equals(r.status) ? GREEN : "PENDING".equals(r.status) ? Color.rgb(255, 152, 0) : Color.rgb(33, 150, 243));
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusView.setPadding(dp(8), dp(4), dp(8), dp(4));
        statusView.setBackground(shape(Color.rgb(245, 247, 243), 0));
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        card.addView(statusView, statusParams);
    }

    private void renderHeader() {
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.topMargin = dp(30); headerParams.bottomMargin = dp(12);
        content.addView(headerRow, headerParams);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        headerRow.addView(titleCol);

        TextView heading = new TextView(this);
        heading.setText(getString(R.string.operator_dashboard_title));
        heading.setTextSize(32); heading.setTextColor(INK); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleCol.addView(heading);

        TextView name = new TextView(this);
        name.setText(operators.getFullName() != null ? operators.getFullName() : "Operator");
        name.setTextSize(24); name.setTextColor(INK); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
