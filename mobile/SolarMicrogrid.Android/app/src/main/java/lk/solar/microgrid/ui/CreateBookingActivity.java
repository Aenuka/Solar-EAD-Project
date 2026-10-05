package lk.solar.microgrid.ui;

import android.app.AlertDialog;
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

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.Station;

import lk.solar.microgrid.R;
import lk.solar.microgrid.data.BookingText;
import lk.solar.microgrid.data.BookingWindow;
import lk.solar.microgrid.data.StationChoices;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Create a new reservation via the central API.
 * Author: Sajith
 */
public final class CreateBookingActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;

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
        if (state == null && intent.hasExtra("stationId") && intent.hasExtra("slotId")) {
            selectedStationId = intent.getStringExtra("stationId");
            selectedSlotId = intent.getStringExtra("slotId");
            selectedSlotStartsAt = intent.getStringExtra("startsAt");
            availableEnergyKwh = intent.getDoubleExtra("availableEnergyKwh", 0);

            stationLabel.setText(intent.getStringExtra("stationName"));
            stationLabel.setTextColor(INK);

            slotLabel.setText(BookingText.window(selectedSlotStartsAt, intent.getStringExtra("endsAt")) + "\n" + availableEnergyKwh + " kWh available");
            slotLabel.setTextColor(INK);
            energyHint.setText("Available: " + availableEnergyKwh + " kWh");
            loadStationDetail(selectedStationId);
        }
        if (state != null && state.getString("stationId") != null) {
            selectedStationId = state.getString("stationId"); selectedSlotId = state.getString("slotId");
            selectedSlotStartsAt = state.getString("startsAt"); availableEnergyKwh = state.getDouble("availableEnergy");
            stationLabel.setText(state.getString("stationLabel")); slotLabel.setText(state.getString("slotLabel"));
            energy.setText(state.getString("energy", ""));
            loadStationDetail(selectedStationId);
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("stationId", selectedStationId); state.putString("slotId", selectedSlotId);
        state.putString("startsAt", selectedSlotStartsAt); state.putDouble("availableEnergy", availableEnergyKwh);
        state.putString("stationLabel", stationLabel.getText().toString()); state.putString("slotLabel", slotLabel.getText().toString());
        state.putString("energy", energy.getText().toString());
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

        TextView heading = text(getString(R.string.create_booking), 32, INK, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(8); headingLayout.bottomMargin = dp(12);
        text("Choose a station and an energy window in the next 7 days. Times are in Sri Lanka time.", 14, MUTED, false);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        message = text("", 13, INK, false);
        message.setVisibility(View.GONE);

        TextView stationCaption = text("Station", 12, INK, true);
        ((LinearLayout.LayoutParams) stationCaption.getLayoutParams()).topMargin = dp(18);
        stationLabel = text("No station selected", 14, MUTED, false);
        button("Select Station", false, this::pickStation);

        TextView slotCaption = text("Energy window", 12, INK, true);
        ((LinearLayout.LayoutParams) slotCaption.getLayoutParams()).topMargin = dp(18);
        slotLabel = text("No energy window selected", 14, MUTED, false);
        button("Choose energy window", false, this::pickSlot);

        energy = field("Energy Amount (kWh)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 12);
        energy.setHint("e.g. 25");

        energyHint = text("Available: -", 12, MUTED, false);

        button("Create Booking", true, this::submit);
    }

    private void pickStation() {
        setBusy(true);
        StationChoices.load(((SolarApplication) getApplication()).accounts(), new AccountRepository.Callback<List<Station>>() {
            @Override public void success(List<Station> list) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                try {
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
        currentStationDetail = null;
        selectedStationId = station.id;
        stationLabel.setText(station.name + "\n" + station.address);
        stationLabel.setTextColor(INK);

        selectedSlotId = null;
        selectedSlotStartsAt = null;
        availableEnergyKwh = 0;
        slotLabel.setText("No energy window selected");
        slotLabel.setTextColor(MUTED);
        energyHint.setText("Available: -");

        loadStationDetail(station.id);
    }

    private void loadStationDetail(String id) {
        setBusy(true);
        ((SolarApplication) getApplication()).accounts().station(id, new AccountRepository.Callback<JSONObject>() {
            @Override public void success(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                try {
                    if (!id.equals(selectedStationId)) return;
                    currentStationDetail = new Station(response);
                    stationLabel.setText(currentStationDetail.name + "\n" + currentStationDetail.address);
                    if (selectedSlotId != null) {
                        BookingWindow selected = null;
                        for (BookingWindow window : BookingWindow.available(response.getJSONArray("slots"), null, 0, java.time.Instant.now()))
                            if (window.id.equals(selectedSlotId)) selected = window;
                        if (selected != null) {
                            selectedSlotStartsAt = selected.startsAt; availableEnergyKwh = selected.availableEnergy;
                            slotLabel.setText(selected.label()); energyHint.setText("Available: " + availableEnergyKwh + " kWh");
                        } else {
                            selectedSlotId = null; selectedSlotStartsAt = null; availableEnergyKwh = 0;
                            slotLabel.setText("Choose an available energy window"); energyHint.setText("Available: -");
                        }
                    }
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
            List<BookingWindow> availableSlots = BookingWindow.available(currentStationDetail.source.getJSONArray("slots"), null, 0, java.time.Instant.now());
            if (availableSlots.isEmpty()) {
                showMessage("No available slots found for this station.", true);
                return;
            }

            String[] display = new String[availableSlots.size()];
            for (int i = 0; i < availableSlots.size(); i++) display[i] = availableSlots.get(i).label();

            new AlertDialog.Builder(CreateBookingActivity.this)
                .setTitle("Choose energy window")
                .setItems(display, (dialog, which) -> {
                    try {
                        BookingWindow window = availableSlots.get(which);
                        selectedSlotId = window.id;
                        selectedSlotStartsAt = window.startsAt;
                        availableEnergyKwh = window.availableEnergy;
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
        if (selectedSlotId == null || selectedSlotStartsAt == null) { showMessage("Choose an energy window", true); return; }
        String enStr = energy.getText().toString().trim();

        double en;
        try {
            en = Double.parseDouble(enStr);
            if (!Double.isFinite(en) || en <= 0) throw new NumberFormatException();
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
                        .setMessage(r.stationLabel() +
                                    "\n" + r.windowLabel() +
                                    "\nEnergy: " + r.energyAmountKwh + " kWh" +
                                    "\nStatus: " + r.status.substring(0, 1) + r.status.substring(1).toLowerCase(java.util.Locale.ROOT) +
                                    "\n\nBooking reference: " + r.reservationId)
                        .setPositiveButton("View bookings", (d, w) -> SolarNavigation.open(CreateBookingActivity.this, SolarNavigation.Tab.BOOKINGS))
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
        v.setText(value); SolarStyle.text(v, size, color, bold);
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
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(8)); bg.setStroke(dp(1), SolarStyle.BORDER);
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
        message.setTextColor(error ? SolarStyle.RED : GREEN);
        message.setVisibility(View.VISIBLE);
    }

    private static String value(EditText input) { return input.getText().toString(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
