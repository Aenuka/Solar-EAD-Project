package lk.solar.microgrid.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Dedicated cancel screen for a single reservation.
 * Author: Sajith
 */
public final class CancelBookingActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN,
            INK = SolarStyle.INK,
            MUTED = SolarStyle.MUTED,
            RED = SolarStyle.RED;

    private ReservationRepository reservations;
    private LinearLayout content;
    private ProgressBar progress;
    private TextView message;
    private EditText reasonField;
    private Button confirmBtn;
    private final List<Button> actions = new ArrayList<>();

    private String reservationId, reservationCode;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        reservationId = getIntent().getStringExtra("reservationId");
        reservationCode = getIntent().getStringExtra("reservationCode");

        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView heading = text(getString(R.string.cancel_booking), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(8);
        text("Reservation: " + (reservationCode == null ? reservationId : reservationCode),
                14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        message = text("", 13, INK, false);
        message.setVisibility(View.GONE);

        // Reason field
        TextView cap = text(getString(R.string.cancel_reason_label), 12, INK, true);
        ((LinearLayout.LayoutParams) cap.getLayoutParams()).topMargin = dp(18);
        reasonField = new EditText(this);
        reasonField.setTextSize(15); reasonField.setTextColor(INK); reasonField.setHintTextColor(MUTED);
        reasonField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        reasonField.setMinLines(3);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), SolarStyle.BORDER);
        reasonField.setBackground(bg);
        reasonField.setPadding(dp(13), dp(12), dp(13), dp(12));
        SolarStyle.field(reasonField);
        content.addView(reasonField, new LinearLayout.LayoutParams(-1, -2));

        // Confirm button
        confirmBtn = new Button(this);
        confirmBtn.setText(R.string.cancel_booking);
        confirmBtn.setTextSize(13); confirmBtn.setAllCaps(false);
        confirmBtn.setTextColor(Color.WHITE);
        SolarStyle.danger(confirmBtn);
        confirmBtn.setMinHeight(dp(52));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(20);
        content.addView(confirmBtn, lp);
        actions.add(confirmBtn);
        confirmBtn.setOnClickListener(v -> { if (!busy) confirmDialog(); });

        // Back button
        Button back = new Button(this);
        back.setText(R.string.back_account);
        back.setTextSize(13); back.setAllCaps(false);
        back.setTextColor(GREEN);
        SolarStyle.button(back, false);
        back.setMinHeight(dp(52));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = dp(12);
        content.addView(back, blp);
        actions.add(back);
        back.setOnClickListener(v -> finish());
    }

    private void confirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_cancel)
                .setMessage(getString(R.string.cancel_reason_label) + ": "
                        + (value(reasonField).isEmpty() ? "(none)" : value(reasonField)))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.cancel_booking, (d, w) -> doCancel())
                .show();
    }

    private void doCancel() {
        setBusy(true);
        String reason = value(reasonField).trim();
        reservations.cancel(reservationId, reason, new ReservationRepository.Callback<Reservation>() {
            @Override public void success(Reservation r) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                new AlertDialog.Builder(CancelBookingActivity.this)
                        .setTitle(R.string.booking_cancelled)
                        .setMessage("Reservation " + (r.reservationId.isEmpty() ? r.id : r.reservationId)
                                + " has been cancelled.")
                        .setPositiveButton(R.string.ok_button, (d, w) -> finish())
                        .setCancelable(false)
                        .show();
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                if (status == 401) {
                    Toast.makeText(CancelBookingActivity.this, text, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    showMessage(text, true);
                }
            }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        for (Button b : actions) b.setEnabled(!value);
        reasonField.setEnabled(!value);
    }

    private void showMessage(String value, boolean error) {
        message.setText(value);
        message.setTextColor(error ? RED : GREEN);
        message.setVisibility(View.VISIBLE);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); SolarStyle.text(v, size, color, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        content.addView(v, lp);
        return v;
    }

    private static String value(EditText input) { return input.getText().toString(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
