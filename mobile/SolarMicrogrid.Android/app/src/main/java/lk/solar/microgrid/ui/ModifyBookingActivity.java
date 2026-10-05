package lk.solar.microgrid.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.BookingText;
import lk.solar.microgrid.data.BookingWindow;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;
import org.json.JSONObject;

/** Edit with named station details and live selectable energy windows. */
public final class ModifyBookingActivity extends SolarActivity {
    private LinearLayout content;
    private TextView stationLabel, windowLabel, message;
    private EditText energy;
    private ProgressBar progress;
    private final List<Button> actions = new ArrayList<>();
    private List<BookingWindow> windows = new ArrayList<>();
    private String reservationId, stationId, originalSlotId, selectedSlotId, reservationDate, tradingType;
    private double originalEnergy;
    private BookingWindow selectedWindow;
    private boolean busy, loaded;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservationId = getIntent().getStringExtra("reservationId");
        stationId = getIntent().getStringExtra("stationId");
        originalSlotId = getIntent().getStringExtra("slotId");
        originalEnergy = getIntent().getDoubleExtra("energyAmountKwh", 0);
        tradingType = getIntent().getStringExtra("tradingType");
        selectedSlotId = state == null ? originalSlotId : state.getString("selectedSlotId", originalSlotId);
        reservationDate = state == null ? getIntent().getStringExtra("reservationDate") : state.getString("reservationDate");

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2)); setContentView(scroll);
        SolarStyle.hero(content, "Edit booking", "Choose an energy window and the amount to transfer. Times are in Sri Lanka time.");
        stationLabel = text(getIntent().getStringExtra("stationName"), 22, true);
        if (stationLabel.getText().length() == 0) stationLabel.setText("Loading station…");
        text(getIntent().getStringExtra("stationAddress"), 16, false);
        text("Energy window", 16, true);
        windowLabel = text(BookingText.window(getIntent().getStringExtra("slotStartsAt"), getIntent().getStringExtra("slotEndsAt")), 16, false);
        button("Choose energy window", false, this::pickWindow);
        text("Energy amount (kWh)", 16, true);
        energy = new EditText(this);
        energy.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        energy.setText(state == null ? String.valueOf(originalEnergy) : state.getString("energy", String.valueOf(originalEnergy)));
        SolarStyle.field(energy); content.addView(energy, new LinearLayout.LayoutParams(-1, -2));
        text("Changes require at least 12 hours before your current window starts.", 14, false);
        message = text("", 14, false); message.setVisibility(View.GONE);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        button("Reload availability", false, this::loadWindows);
        button("Save changes", true, this::confirmUpdate);
        loadWindows();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("selectedSlotId", selectedSlotId); state.putString("reservationDate", reservationDate);
        state.putString("energy", energy.getText().toString());
    }

    private void loadWindows() {
        if (stationId == null || reservationId == null) { showMessage("Booking unavailable. Return to Bookings and try again."); return; }
        loaded = false; setBusy(true);
        ((SolarApplication) getApplication()).accounts().station(stationId, new AccountRepository.Callback<JSONObject>() {
            @Override public void success(JSONObject response) {
                if (!alive()) return;
                try {
                    stationLabel.setText(response.getString("name"));
                    windows = BookingWindow.available(response.getJSONArray("slots"), originalSlotId, originalEnergy, Instant.now());
                    selectedWindow = null;
                    for (BookingWindow window : windows) if (window.id.equals(selectedSlotId)) selectedWindow = window;
                    loaded = true;
                    if (selectedWindow != null) windowLabel.setText(selectedWindow.label());
                    else { windowLabel.setText("Choose an available energy window"); showMessage("The selected window is unavailable. Choose another window or reload availability."); }
                } catch (Exception e) { showMessage(getString(R.string.contract_error)); }
                setBusy(false);
            }
            @Override public void failure(int status, String error) {
                if (!alive()) return; setBusy(false); showMessage(error);
                if (status == 401) returnToSignIn();
            }
        });
    }

    private void pickWindow() {
        if (!loaded) { showMessage("Reload availability to choose an energy window."); return; }
        if (windows.isEmpty()) { showMessage("No available energy windows in the next seven days."); return; }
        String[] labels = new String[windows.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = windows.get(i).label();
        new AlertDialog.Builder(this).setTitle("Choose energy window").setItems(labels, (dialog, which) -> {
            BookingWindow window = windows.get(which);
            if (!window.id.equals(selectedSlotId)) reservationDate = window.startsAt;
            selectedSlotId = window.id; selectedWindow = window; windowLabel.setText(window.label());
        }).setNegativeButton(R.string.cancel, null).show();
    }

    private void confirmUpdate() {
        if (!loaded || selectedWindow == null) { showMessage("Choose an available energy window first."); return; }
        double amount;
        try {
            amount = Double.parseDouble(energy.getText().toString().trim());
            if (!Double.isFinite(amount) || amount <= 0 || amount > selectedWindow.availableEnergy) throw new NumberFormatException();
        } catch (NumberFormatException e) { energy.setError("Enter an amount up to " + selectedWindow.availableEnergy + " kWh"); return; }
        new AlertDialog.Builder(this).setTitle("Save booking changes?")
                .setMessage(stationLabel.getText() + "\n" + BookingText.window(selectedWindow.startsAt, selectedWindow.endsAt) + "\n" + amount + " kWh")
                .setNegativeButton(R.string.cancel, null).setPositiveButton("Save changes", (d, w) -> submit(amount)).show();
    }

    private void submit(double amount) {
        setBusy(true);
        ((SolarApplication) getApplication()).reservations().update(reservationId, selectedSlotId, reservationDate,
                amount, tradingType == null ? "DROP_OFF" : tradingType, new ReservationRepository.Callback<Reservation>() {
            @Override public void success(Reservation booking) {
                if (!alive()) return; setBusy(false);
                new AlertDialog.Builder(ModifyBookingActivity.this).setTitle("Booking updated")
                        .setMessage(booking.stationLabel() + "\n" + booking.windowLabel() + "\n" + booking.energyAmountKwh + " kWh")
                        .setPositiveButton("View bookings", (d, w) -> SolarNavigation.open(ModifyBookingActivity.this, SolarNavigation.Tab.BOOKINGS))
                        .setCancelable(false).show();
            }
            @Override public void failure(int status, String error) {
                if (!alive()) return; setBusy(false); showMessage(error);
                if (status == 401) returnToSignIn();
            }
        });
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value);
        SolarStyle.text(view, size, bold ? SolarStyle.INK : SolarStyle.MUTED, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12);
        content.addView(view, params); return view;
    }
    private void button(String label, boolean primary, Runnable action) {
        Button button = new Button(this); button.setText(label); SolarStyle.button(button, primary);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(12);
        content.addView(button, params); actions.add(button);
        button.setOnClickListener(v -> { if (!busy) action.run(); });
    }
    private void setBusy(boolean value) {
        busy = value; progress.setVisibility(value ? View.VISIBLE : View.GONE); energy.setEnabled(!value);
        for (Button action : actions) action.setEnabled(!value);
    }
    private void showMessage(String value) { message.setText(value); SolarStyle.notice(message, true); message.setVisibility(View.VISIBLE); }
    private boolean alive() { return !isFinishing() && !isDestroyed(); }
    private int dp(int value) { return SolarStyle.dp(this, value); }
}
