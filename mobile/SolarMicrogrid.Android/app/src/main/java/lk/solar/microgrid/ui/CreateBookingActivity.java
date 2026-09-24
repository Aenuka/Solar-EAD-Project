package lk.solar.microgrid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
 * Create a new reservation via the central API.
 * Author: Sajith
 */
public final class CreateBookingActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);

    private ReservationRepository reservations;
    private LinearLayout content;
    private TextView message;
    private ProgressBar progress;
    private final List<Button> actions = new ArrayList<>();
    private final List<EditText> fields = new ArrayList<>();

    private EditText stationId, slotId, energy;
    private TextView pickedDate;
    private LocalDateTime pickedDateTime;
    private boolean busy;

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 243));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(26), dp(28), dp(26), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView brand = text(getString(R.string.brand), 12, GREEN, true);
        brand.setLetterSpacing(0.13f);
        TextView heading = text(getString(R.string.create_booking), 32, INK, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(12);
        text("Pick a station, a slot, and a time within the next 7 days.", 14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        message = text("", 13, INK, false);
        message.setVisibility(View.GONE);

        stationId = field("Station ID", "", InputType.TYPE_CLASS_TEXT, 60);
        stationId.setHint("e.g. ST001");
        slotId = field("Slot ID", "", InputType.TYPE_CLASS_TEXT, 60);
        slotId.setHint("e.g. SLOT001");

        TextView dateCaption = text("Reservation Date & Time", 12, INK, true);
        ((LinearLayout.LayoutParams) dateCaption.getLayoutParams()).topMargin = dp(18);
        pickedDate = text("No date picked yet", 13, MUTED, false);
        Button pickDate = button("Pick Date & Time", false, this::showDateTimePicker);

        energy = field("Energy Amount (kWh)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 12);
        energy.setHint("e.g. 25");

        button("Create Booking", true, this::submit);
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (dateView, y, m, d) ->
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                pickedDateTime = LocalDateTime.of(y, m + 1, d, hour, minute);
                pickedDate.setText(pickedDateTime.format(DISPLAY));
                pickedDate.setTextColor(INK);
            }, 12, 0, true).show(),
            now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void submit() {
        String stId = value(stationId).trim();
        String slId = value(slotId).trim();
        String enStr = value(energy).trim();

        if (stId.isEmpty()) { stationId.setError("Station ID required"); return; }
        if (slId.isEmpty()) { slotId.setError("Slot ID required"); return; }
        if (pickedDateTime == null) { showMessage("Please pick a date and time", true); return; }

        double en;
        try {
            en = Double.parseDouble(enStr);
            if (en <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            energy.setError("Enter a valid energy amount");
            return;
        }

        // Convert local -> UTC ISO 8601 (e.g. 2026-09-25T04:30:00Z)
        String isoUtc = pickedDateTime
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        setBusy(true);
        reservations.create(stId, slId, isoUtc, en, "DROP_OFF",
            new ReservationRepository.Callback<Reservation>() {
                @Override public void success(Reservation r) {
                    if (isFinishing() || isDestroyed()) return;
                    setBusy(false);
                    new AlertDialog.Builder(CreateBookingActivity.this)
                        .setTitle("Booking Created")
                        .setMessage("Reservation: " + r.reservationId +
                                    "\nStation: " + r.stationId +
                                    "\nEnergy: " + r.energyAmountKwh + " kWh" +
                                    "\nStatus: " + r.status)
                        .setPositiveButton("OK", (d, w) -> finish())
                        .setCancelable(false).show();
                }
                @Override public void failure(int status, String text) {
                    if (isFinishing() || isDestroyed()) return;
                    setBusy(false);
                    if (status == 401) {
                        Toast.makeText(CreateBookingActivity.this, text, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        showMessage(text, true);
                    }
                }
            });
    }

    // ---- Helper views (same as MainActivity) ----

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        content.addView(v, lp);
        return v;
    }

    private EditText field(String captionText, String initial, int inputType, int maxLength) {
        TextView caption = text(captionText, 12, INK, true);
        ((LinearLayout.LayoutParams) caption.getLayoutParams()).topMargin = dp(18);
        EditText editor = new EditText(this);
        editor.setTextSize(15); editor.setTextColor(INK); editor.setHintTextColor(MUTED);
        editor.setInputType(inputType); editor.setText(initial);
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(8)); bg.setStroke(dp(1), Color.rgb(213, 224, 214));
        editor.setBackground(bg);
        editor.setPadding(dp(13), dp(12), dp(13), dp(12));
        editor.setMinimumHeight(dp(50));
        content.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        fields.add(editor);
        return editor;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : GREEN);
        b.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(234, 240, 227)));
        b.setMinHeight(dp(50));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(15);
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
        message.setTextColor(error ? Color.rgb(155, 66, 44) : GREEN);
        message.setVisibility(View.VISIBLE);
    }

    private static String value(EditText input) { return input.getText().toString(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}