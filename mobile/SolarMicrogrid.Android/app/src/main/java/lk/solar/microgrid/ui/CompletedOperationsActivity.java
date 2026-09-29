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

public class CompletedOperationsActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);
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
            loadOperations();
        }
    }

    private void loadOperations() {
        content.removeAllViews();
        
        TextView heading = new TextView(this);
        heading.setText("Completed Operations");
        heading.setTextSize(32); heading.setTextColor(INK); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams headingLayout = new LinearLayout.LayoutParams(-1, -2);
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(4);
        content.addView(heading, headingLayout);

        TextView sub = new TextView(this);
        sub.setText("Previously finalized energy transfers");
        sub.setTextSize(14); sub.setTextColor(MUTED);
        LinearLayout.LayoutParams subLayout = new LinearLayout.LayoutParams(-1, -2);
        subLayout.bottomMargin = dp(24);
        content.addView(sub, subLayout);

        TextView loading = new TextView(this);
        loading.setText("Loading completed operations...");
        loading.setTextSize(16); loading.setTextColor(MUTED);
        content.addView(loading);

        operators.searchCompletedOperations(new OperatorRepository.Callback<List<Reservation>>() {
            @Override
            public void success(List<Reservation> result) {
                if (isDestroyed() || isFinishing()) return;
                content.removeViewAt(content.getChildCount() - 1);
                
                if (result.isEmpty()) {
                    TextView empty = new TextView(CompletedOperationsActivity.this);
                    empty.setText("No completed operations yet.");
                    empty.setTextSize(14); empty.setTextColor(MUTED);
                    content.addView(empty);
                } else {
                    for (Reservation r : result) {
                        renderCard(r);
                    }
                }
            }

            @Override
            public void failure(int status, String message) {
                if (isDestroyed() || isFinishing()) return;
                content.removeViewAt(content.getChildCount() - 1);
                
                TextView error = new TextView(CompletedOperationsActivity.this);
                error.setText(message != null ? message : "Unable to load completed operations. Please try again.");
                error.setTextSize(16); error.setTextColor(Color.RED);
                content.addView(error);
            }
        });
    }

    private void renderCard(Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(Color.WHITE, Color.rgb(213, 224, 214)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setClickable(true);
        card.setOnClickListener(v -> showDetailsDialog(r));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(12);
        content.addView(card, params);

        TextView idView = new TextView(this);
        idView.setText(r.reservationId); idView.setTextSize(16); idView.setTextColor(INK); idView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(idView);

        TextView stationView = new TextView(this);
        stationView.setText(r.stationId); // Using stationId as requested if name unavailable without new infra
        stationView.setTextSize(14); stationView.setTextColor(INK);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4);
        card.addView(stationView, sp);

        TextView energyView = new TextView(this);
        energyView.setText(r.energyAmountKwh + " kWh");
        energyView.setTextSize(14); energyView.setTextColor(INK);
        card.addView(energyView);

        TextView detailsView = new TextView(this);
        String completedDate = r.source.optString("completedAt", "");
        if (completedDate.isEmpty()) completedDate = r.source.optString("updatedAt", "");
        
        detailsView.setText("Completed\n" + completedDate);
        detailsView.setTextSize(12); detailsView.setTextColor(MUTED);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
        detailsParams.topMargin = dp(8); detailsParams.bottomMargin = dp(8);
        card.addView(detailsView, detailsParams);

        TextView statusView = new TextView(this);
        statusView.setText("COMPLETED");
        statusView.setTextSize(12); 
        statusView.setTextColor(GREEN);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusView.setPadding(dp(8), dp(4), dp(8), dp(4));
        statusView.setBackground(shape(Color.rgb(245, 247, 243), 0));
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        card.addView(statusView, statusParams);
    }

    private void showDetailsDialog(Reservation r) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(layout);

        detail(layout, "Booking", r.reservationId);
        detail(layout, "Prosumer", r.prosumerNic);
        detail(layout, "Station", r.stationId);
        detail(layout, "Energy", r.energyAmountKwh + " kWh");
        detail(layout, "Trading Type", r.tradingType);
        detail(layout, "Booking Time", r.reservationDate);
        detail(layout, "Status", r.status);
        String completedAt = r.source.optString("completedAt", r.source.optString("updatedAt", "Unknown"));
        detail(layout, "Completed At", completedAt);

        new AlertDialog.Builder(this)
            .setTitle("Completed Operation Details")
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void detail(LinearLayout parent, String label, String value) {
        TextView viewLabel = new TextView(this);
        viewLabel.setText(label);
        viewLabel.setTextSize(12);
        viewLabel.setTextColor(MUTED);
        parent.addView(viewLabel);
        
        TextView viewValue = new TextView(this);
        viewValue.setText(value);
        viewValue.setTextSize(16);
        viewValue.setTextColor(INK);
        viewValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(16);
        parent.addView(viewValue, layout);
    }

    private android.graphics.drawable.GradientDrawable shape(int fill, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable(); 
        shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
