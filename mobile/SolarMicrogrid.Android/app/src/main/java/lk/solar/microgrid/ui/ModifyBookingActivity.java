package lk.solar.microgrid.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Modify an existing reservation via the API (PUT /reservations/{id}).
 * Author: Sajith
 */
public final class ModifyBookingActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN,
            INK = SolarStyle.INK,
            MUTED = SolarStyle.MUTED,
            RED = SolarStyle.RED;

    private ReservationRepository reservations;
    private LinearLayout content;
    private ProgressBar progress;
    private TextView message, pickedDateText;
    private final List<Button> actions = new ArrayList<>();
    private final List<EditText> fields = new ArrayList<>();

    private String reservationId, stationId;
    private EditText slotIdField, energyField;
    private LocalDateTime pickedDateTime;
    private boolean busy;

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();

        // Read data passed from history list
        reservationId = getIntent().getStringExtra("reservationId");
        stationId = getIntent().getStringExtra("stationId");
        String originalSlotId = getIntent().getStringExtra("slotId");
        String originalDate = getIntent().getStringExtra("reservationDate");
        double originalEnergy = getIntent().getDoubleExtra("energyAmountKwh", 0);
        String originalTradingType = getIntent().getStringExtra("tradingType");

        buildUi(originalSlotId, originalDate, originalEnergy, originalTradingType);
    }

    private void buildUi(String originalSlotId, String originalDate, double originalEnergy, String originalTrading) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView heading = text(getString(R.string.modify_booking), 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(8);
        text("Update your reservation details.", 14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        message = text("", 13, INK, false);
        message.setVisibility(View.GONE);

        // Station (read-only display)
        text("Station: " + (stationId == null ? "" : stationId), 14, INK, true);
        text("Trading type: " + (originalTrading == null ? "DROP_OFF" : originalTrading), 13, MUTED, false);

        // Slot ID
        slotIdField = field(getString(R.string.slot_id_label),
                originalSlotId == null ? "" : originalSlotId,
                InputType.TYPE_CLASS_TEXT, 60);

        // Date
        TextView dateCaption = text(getString(R.string.reservation_date_label), 12, INK, true);
        ((LinearLayout.LayoutParams) dateCaption.getLayoutParams()).topMargin = dp(18);
        pickedDateText = text(originalDate == null || originalDate.isEmpty()
                ? getString(R.string.no_date_picked) : originalDate, 13, MUTED, false);

        // Parse original date if possible
        try {
            if (originalDate != null && !originalDate.isEmpty()) {
                pickedDateTime = LocalDateTime.parse(
                        originalDate.replace("Z", "").substring(0, 19),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception ignored) { }

        button(getString(R.string.pick_date_time), false, this::showDateTimePicker);

        // Energy
        energyField = field(getString(R.string.energy_amount_label),
                originalEnergy > 0 ? String.valueOf(originalEnergy) : "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 12);

        // Submit
        button(getString(R.string.update_booking), true, this::confirmUpdate);
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (dateView, y, m, d) ->
                new TimePickerDialog(this, (timeView, hour, minute) -> {
                    pickedDateTime = LocalDateTime.of(y, m + 1, d, hour, minute);
                    pickedDateText.setText(pickedDateTime.format(DISPLAY));
                    pickedDateText.setTextColor(INK);
                }, 12, 0, true).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void confirmUpdate() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_update)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.update_booking, (d, w) -> submit())
                .show();
    }

    private void submit() {
        String newSlotId = value(slotIdField).trim();
        String energyStr = value(energyField).trim();

        if (newSlotId.isEmpty()) { slotIdField.setError(getString(R.string.required_fields)); return; }

        double energyValue;
        try {
            energyValue = Double.parseDouble(energyStr);
            if (energyValue <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            energyField.setError(getString(R.string.energy_amount_label));
            return;
        }

        if (pickedDateTime == null) { showMessage(getString(R.string.validation_date), true); return; }

        String isoUtc = pickedDateTime
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        setBusy(true);
        reservations.update(reservationId, newSlotId, isoUtc, energyValue, "DROP_OFF",
                new ReservationRepository.Callback<Reservation>() {
                    @Override public void success(Reservation r) {
                        if (isFinishing() || isDestroyed()) return;
                        setBusy(false);
                        new AlertDialog.Builder(ModifyBookingActivity.this)
                                .setTitle(R.string.booking_updated_title)
                                .setMessage(getString(R.string.booking_updated_message)
                                        + "\n\nReservation: " + r.reservationId
                                        + "\nSlot: " + r.slotId
                                        + "\nDate: " + r.reservationDate)
                                .setPositiveButton(R.string.ok_button, (d, w) -> finish())
                                .setCancelable(false)
                                .show();
                    }
                    @Override public void failure(int status, String text) {
                        if (isFinishing() || isDestroyed()) return;
                        setBusy(false);
                        if (status == 401) {
                            Toast.makeText(ModifyBookingActivity.this, text, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            showMessage(text, true);
                        }
                    }
                });
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); SolarStyle.text(v, size, color, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        content.addView(v, lp);
        return v;
    }

    private EditText field(String caption, String initial, int inputType, int maxLength) {
        TextView cap = text(caption, 12, INK, true);
        ((LinearLayout.LayoutParams) cap.getLayoutParams()).topMargin = dp(18);
        EditText editor = new EditText(this);
        editor.setTextSize(15); editor.setTextColor(INK); editor.setHintTextColor(MUTED);
        editor.setInputType(inputType); editor.setText(initial);
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), SolarStyle.BORDER);
        editor.setBackground(bg);
        editor.setPadding(dp(13), dp(12), dp(13), dp(12));
        SolarStyle.field(editor);
        content.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        fields.add(editor);
        return editor;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : GREEN);
        SolarStyle.button(b, primary);
        b.setMinHeight(dp(52));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        content.addView(b, lp);
        actions.add(b);
        b.setOnClickListener(v -> { if (!busy) action.run(); });
        return b;
    }

    private void setBusy(boolean value) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        for (Button b : actions) b.setEnabled(!value);
        for (EditText f : fields) f.setEnabled(!value);
    }

    private void showMessage(String value, boolean error) {
        message.setText(value);
        message.setTextColor(error ? RED : GREEN);
        message.setVisibility(View.VISIBLE);
    }

    private static String value(EditText input) { return input.getText().toString(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
