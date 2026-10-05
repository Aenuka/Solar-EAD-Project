package lk.solar.microgrid.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import lk.solar.microgrid.R;
import lk.solar.microgrid.data.BookingText;
import lk.solar.microgrid.data.Reservation;

public class TransactionVerificationActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String json = getIntent().getStringExtra("reservationJson");
        if (json == null) {
            finish();
            return;
        }

        Reservation reservation;
        try {
            reservation = new Reservation(new JSONObject(json));
        } catch (Exception e) {
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

        TextView heading = text(getString(R.string.transaction_verified_title), 24, GREEN, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(8); headingLayout.bottomMargin = dp(20);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        SolarStyle.card(card);
        card.setElevation(0);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));

        detail(card, "Booking:", reservation.reservationId);
        detail(card, "Prosumer:", reservation.prosumerLabel());
        detail(card, "Station:", reservation.stationLabel());
        detail(card, "Booking Time:", BookingText.date(reservation.reservationDate));
        detail(card, "Energy window (Sri Lanka):", reservation.windowLabel());
        detail(card, "Energy:", reservation.energyAmountKwh + " kWh");
        detail(card, "Trading Type:", BookingText.trading(reservation.tradingType));
        detail(card, "Status:", reservation.status);

        Button button = new Button(this);
        button.setText(R.string.back_to_scanner);
        button.setTextSize(13); button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(GREEN);
        SolarStyle.button(button, false);
        button.setMinHeight(dp(52));
        LinearLayout.LayoutParams layout2 = new LinearLayout.LayoutParams(-1, -2);
        layout2.topMargin = dp(15);

        Button finalizeButton = new Button(this);
        finalizeButton.setText("Finalize Energy Transfer");
        finalizeButton.setTextSize(13); finalizeButton.setAllCaps(false);
        finalizeButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        finalizeButton.setTextColor(Color.WHITE);
        SolarStyle.button(finalizeButton, true);
        finalizeButton.setElevation(0);
        finalizeButton.setMinHeight(dp(52));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(30);

        content.addView(finalizeButton, layout);
        content.addView(button, layout2);
        button.setOnClickListener(view -> finish());

        finalizeButton.setOnClickListener(view -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Finalize Energy Transfer")
                .setMessage("This action will mark the reservation as completed.")
                .setPositiveButton("Finalize", (dialog, which) -> {
                    finalizeButton.setEnabled(false);
                    finalizeButton.setText("Completing...");

                    ((lk.solar.microgrid.SolarApplication) getApplication()).operators().completeTransaction(reservation.id, new lk.solar.microgrid.data.OperatorRepository.Callback<Reservation>() {
                        @Override
                        public void success(Reservation result) {
                            if (isDestroyed() || isFinishing()) return;
                            showCompletedState(result);
                        }

                        @Override
                        public void failure(int status, String message) {
                            if (isDestroyed() || isFinishing()) return;
                            finalizeButton.setEnabled(true);
                            finalizeButton.setText("Finalize Energy Transfer");
                            new android.app.AlertDialog.Builder(TransactionVerificationActivity.this)
                                .setTitle("Error")
                                .setMessage(message != null ? message : "Failed to finalize.")
                                .setPositiveButton("OK", null)
                                .show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void showCompletedState(Reservation result) {
        content.removeAllViews();

        TextView heading = text("✓ Energy Transfer Completed", 24, GREEN, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(8); headingLayout.bottomMargin = dp(20);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        SolarStyle.card(card);
        card.setElevation(0);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));

        detail(card, "Booking:", result.reservationId);
        detail(card, "Station:", result.stationLabel());
        detail(card, "Energy:", result.energyAmountKwh + " kWh");
        detail(card, "Status:", result.status);

        String completedDate = result.source.optString("completedAt", result.source.optString("updatedAt", "Now"));
        detail(card, "Completed (Sri Lanka):", BookingText.date(completedDate));

        Button backButton = new Button(this);
        backButton.setText("Back to Operator Dashboard");
        backButton.setTextSize(13); backButton.setAllCaps(false);
        backButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        backButton.setTextColor(Color.WHITE);
        SolarStyle.button(backButton, true);
        backButton.setElevation(0);
        backButton.setMinHeight(dp(52));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(30);
        content.addView(backButton, layout);
        backButton.setOnClickListener(view -> finish());

        Button scanAnother = new Button(this);
        scanAnother.setText("Scan Another QR");
        scanAnother.setTextSize(13); scanAnother.setAllCaps(false);
        scanAnother.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        scanAnother.setTextColor(GREEN);
        SolarStyle.button(scanAnother, false);
        scanAnother.setMinHeight(dp(52));
        LinearLayout.LayoutParams layout2 = new LinearLayout.LayoutParams(-1, -2);
        layout2.topMargin = dp(15);
        content.addView(scanAnother, layout2);
        scanAnother.setOnClickListener(view -> {
            startActivity(new android.content.Intent(this, QrScannerActivity.class));
            finish();
        });
    }

    private void detail(LinearLayout parent, String label, String value) {
        TextView viewLabel = new TextView(this);
        viewLabel.setText(label);
        viewLabel.setTextSize(13);
        viewLabel.setTextColor(MUTED);
        parent.addView(viewLabel);

        TextView viewValue = new TextView(this);
        viewValue.setText(value);
        viewValue.setTextSize(16);
        viewValue.setTextColor(INK);
        viewValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(16);
        parent.addView(viewValue, layout);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); SolarStyle.text(view, size, color, bold);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
        content.addView(view, layout);
        return view;
    }

    private android.graphics.drawable.GradientDrawable shape(int fill, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
