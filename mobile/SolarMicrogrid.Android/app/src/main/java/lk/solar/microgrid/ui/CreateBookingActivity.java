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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.Station;

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

    private EditText energy;
    private TextView stationLabel, slotLabel, energyHint;
    private String selectedStationId, selectedSlotId, selectedSlotStartsAt;
    private double availableEnergyKwh;
    private Station currentStationDetail;
    private boolean busy;

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        buildUi();

        Intent intent = getIntent();
        if (intent.hasExtra("stationId") && intent.hasExtra("slotId")) {
            selectedStationId = intent.getStringExtra("stationId");
            selectedSlotId = intent.getStringExtra("slotId");
            selectedSlotStartsAt = intent.getStringExtra("startsAt");
            availableEnergyKwh = intent.getDoubleExtra("availableEnergyKwh", 0);

            stationLabel.setText(intent.getStringExtra("stationName") + "\n(Pre-selected)");
            stationLabel.setTextColor(INK);

            OffsetDateTime start = OffsetDateTime.parse(selectedSlotStartsAt).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30));
            OffsetDateTime end = OffsetDateTime.parse(intent.getStringExtra("endsAt")).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30));
            DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
            slotLabel.setText(start.format(date) + "\n" + start.format(time) + " - " + end.format(time) + "\n" + availableEnergyKwh + " kWh available");
            slotLabel.setTextColor(INK);
            energyHint.setText("Available: " + availableEnergyKwh + " kWh");
        }
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

        TextView stationCaption = text("Station", 12, INK, true);
        ((LinearLayout.LayoutParams) stationCaption.getLayoutParams()).topMargin = dp(18);
        stationLabel = text("No station selected", 14, MUTED, false);
        button("Select Station", false, this::pickStation);

        TextView slotCaption = text("Available Slot", 12, INK, true);
        ((LinearLayout.LayoutParams) slotCaption.getLayoutParams()).topMargin = dp(18);
        slotLabel = text("No slot selected", 14, MUTED, false);
        button("Select Available Slot", false, this::pickSlot);

        energy = field("Energy Amount (kWh)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 12);
        energy.setHint("e.g. 25");
        
        energyHint = text("Available: -", 12, MUTED, false);

        button("Create Booking", true, this::submit);
    }

    private void pickStation() {
        setBusy(true);
        ((SolarApplication) getApplication()).accounts().stations("activeOnly=true&pageSize=50", new AccountRepository.Callback<JSONObject>() {
            @Override public void success(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                try {
                    JSONArray items = response.getJSONArray("items");
                    List<Station> list = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) list.add(new Station(items.getJSONObject(i)));
                    if (list.isEmpty()) { showMessage("No stations found.", true); return; }
                    
                    String[] names = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) names[i] = list.get(i).name + "\n" + list.get(i).address;
                    
                    new AlertDialog.Builder(CreateBookingActivity.this)
                        .setTitle("Select Station")
                        .setItems(names, (dialog, which) -> onStationSelected(list.get(which)))
                        .show();
                } catch (Exception e) {
                    showMessage("Error parsing stations", true);
                }
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                showMessage(text, true);
            }
        });
    }

    private void onStationSelected(Station station) {
        selectedStationId = station.id;
        stationLabel.setText(station.name + "\n" + station.address);
        stationLabel.setTextColor(INK);
        
        selectedSlotId = null;
        selectedSlotStartsAt = null;
        availableEnergyKwh = 0;
        slotLabel.setText("No slot selected");
        slotLabel.setTextColor(MUTED);
        energyHint.setText("Available: -");
        
        setBusy(true);
        ((SolarApplication) getApplication()).accounts().station(station.id, new AccountRepository.Callback<JSONObject>() {
            @Override public void success(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                try {
                    currentStationDetail = new Station(response);
                } catch (Exception e) {
                    showMessage("Error parsing station details", true);
                }
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                showMessage(text, true);
            }
        });
    }

    private void pickSlot() {
        if (currentStationDetail == null) {
            showMessage("Please select a station first", true);
            return;
        }
        try {
            JSONArray slots = currentStationDetail.source.getJSONArray("slots");
            List<JSONObject> availableSlots = new ArrayList<>();
            for (int i = 0; i < slots.length(); i++) {
                JSONObject s = slots.getJSONObject(i);
                if (s.getInt("availableSlots") > 0 && s.getDouble("availableEnergyKwh") > 0) {
                    availableSlots.add(s);
                }
            }
            if (availableSlots.isEmpty()) {
                showMessage("No available slots found for this station.", true);
                return;
            }
            
            String[] display = new String[availableSlots.size()];
            DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
            for (int i = 0; i < availableSlots.size(); i++) {
                JSONObject s = availableSlots.get(i);
                OffsetDateTime start = OffsetDateTime.parse(s.getString("startsAt")).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30));
                OffsetDateTime end = OffsetDateTime.parse(s.getString("endsAt")).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5, 30));
                display[i] = start.format(date) + "\n" + start.format(time) + " - " + end.format(time) +
                    "\n" + s.getInt("availableSlots") + " battery slots available\n" +
                    s.getDouble("availableEnergyKwh") + " kWh available";
            }
            
            new AlertDialog.Builder(CreateBookingActivity.this)
                .setTitle("Select Available Slot")
                .setItems(display, (dialog, which) -> {
                    try {
                        JSONObject s = availableSlots.get(which);
                        selectedSlotId = s.getString("id");
                        selectedSlotStartsAt = s.getString("startsAt");
                        availableEnergyKwh = s.getDouble("availableEnergyKwh");
                        slotLabel.setText(display[which]);
                        slotLabel.setTextColor(INK);
                        energyHint.setText("Available: " + availableEnergyKwh + " kWh");
                    } catch (Exception e) { showMessage("Error selecting slot", true); }
                }).show();
        } catch (Exception e) {
            showMessage("Error reading slots", true);
        }
    }

    private void submit() {
        if (selectedStationId == null) { showMessage("Please select a station", true); return; }
        if (selectedSlotId == null || selectedSlotStartsAt == null) { showMessage("Please select a slot", true); return; }
        String enStr = energy.getText().toString().trim();

        double en;
        try {
            en = Double.parseDouble(enStr);
            if (en <= 0) throw new NumberFormatException();
            if (en > availableEnergyKwh) {
                energy.setError("Amount exceeds available energy (" + availableEnergyKwh + " kWh)");
                return;
            }
        } catch (NumberFormatException e) {
            energy.setError("Enter a valid energy amount");
            return;
        }

        // Convert slot start time to UTC ISO 8601 (e.g. 2026-09-25T04:30:00Z)
        String isoUtc = OffsetDateTime.parse(selectedSlotStartsAt)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        setBusy(true);
        reservations.create(selectedStationId, selectedSlotId, isoUtc, en, "DROP_OFF",
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