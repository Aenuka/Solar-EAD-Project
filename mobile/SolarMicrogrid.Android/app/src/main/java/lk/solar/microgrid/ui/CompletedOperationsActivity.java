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
                    int index = 0;
                    for (Reservation r : result) {
                        renderCard(r, index++);
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

    private void renderCard(Reservation r, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(252, 255, 252), Color.WHITE});
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(220, 235, 225));
        card.setBackground(bg);
        card.setElevation(dp(6));
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

        // Header Row: ID and Energy amount
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(headerRow, new LinearLayout.LayoutParams(-1, -2));

        TextView idView = new TextView(this);
        idView.setText(r.reservationId);
        idView.setTextSize(16); idView.setTextColor(INK); idView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        idView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        headerRow.addView(idView);

        TextView energyView = new TextView(this);
        energyView.setText(r.energyAmountKwh + " kWh");
        energyView.setTextSize(16); energyView.setTextColor(GREEN); energyView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerRow.addView(energyView);

        // Subtitle: Station ID
        TextView stationView = new TextView(this);
        String shortStation = r.stationId;
        if (shortStation != null && shortStation.length() > 12) {
            shortStation = shortStation.substring(0, 12) + "...";
        }
        stationView.setText("Station: " + shortStation);
        stationView.setTextSize(13); stationView.setTextColor(MUTED);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(8);
        card.addView(stationView, sp);

        // Date and Status row
        LinearLayout footerRow = new LinearLayout(this);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(16);
        card.addView(footerRow, footerParams);

        TextView detailsView = new TextView(this);
        String completedDate = r.source.optString("completedAt", "");
        if (completedDate.isEmpty()) completedDate = r.source.optString("updatedAt", "");
        completedDate = completedDate.replace("T", " ").replace("Z", "");
        if (completedDate.length() > 19) {
            completedDate = completedDate.substring(0, 19);
        }
        
        detailsView.setText("Done on " + completedDate);
        detailsView.setTextSize(12); detailsView.setTextColor(MUTED);
        detailsView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        footerRow.addView(detailsView);

        TextView statusView = new TextView(this);
        statusView.setText("COMPLETED");
        statusView.setTextSize(11); 
        statusView.setTextColor(GREEN);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusView.setPadding(dp(12), dp(4), dp(12), dp(4));
        android.graphics.drawable.GradientDrawable statusBg = shape(Color.rgb(234, 247, 239), 0);
        statusBg.setCornerRadius(dp(12));
        statusView.setBackground(statusBg);
        
        footerRow.addView(statusView);
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
