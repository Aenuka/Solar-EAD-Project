package lk.solar.microgrid.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
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

import java.util.List;

import lk.solar.microgrid.R;
import lk.solar.microgrid.data.BookingText;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

/**
 * Shows the prosumer's reservation history (live from the API).
 * Author: Sajith
 */
public final class BookingHistoryActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN,
            INK = SolarStyle.INK,
            MUTED = SolarStyle.MUTED,
            AMBER = SolarStyle.AMBER,
            RED = SolarStyle.RED;

    private ReservationRepository reservations;
    private LinearLayout content, listContainer;
    private ProgressBar progress;
    private Button refreshBtn;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private LinearLayout filters;
    private String selectedFilter = "All";
    private List<Reservation> bookings = new java.util.ArrayList<>();
    private boolean loading, cancelling, hasLoaded;
    private int generation;


    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        if (state != null) selectedFilter = state.getString("filter", "All");
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("filter", selectedFilter);
    }

    private void buildUi() {
        swipeRefresh = new androidx.swiperefreshlayout.widget.SwipeRefreshLayout(this);
        swipeRefresh.setColorSchemeColors(SolarStyle.BLUE);
        swipeRefresh.setOnRefreshListener(this::load);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        swipeRefresh.addView(scroll);
        setContentView(swipeRefresh);
        SolarStyle.hero(content, "Bookings", "Your energy transfers, from plan to completion.");
        Button create = button("New booking", true, () -> startActivity(new android.content.Intent(this, CreateBookingActivity.class)));
        ((LinearLayout.LayoutParams) create.getLayoutParams()).topMargin = 0;
        LinearLayout tools = SolarStyle.group(content);
        ((LinearLayout.LayoutParams) tools.getLayoutParams()).topMargin = dp(12);
        SolarStyle.row(tools, R.drawable.ic_nav_explore, "Search bookings", "Find a transfer by station or status",
                () -> startActivity(new android.content.Intent(this, SearchBookingActivity.class)));
        android.widget.HorizontalScrollView filterScroll = new android.widget.HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filters = new LinearLayout(this);
        filters.setPadding(0, dp(4), 0, dp(12));
        filterScroll.addView(filters);
        content.addView(filterScroll);
        renderFilters();
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        refreshBtn = button("Try again", false, this::load);
        refreshBtn.setVisibility(View.GONE);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void renderFilters() {
        filters.removeAllViews();
        for (String filter : new String[]{"All", "Upcoming", "Pending", "Completed"}) {
            TextView chip = new TextView(this);
            chip.setText(filter);
            SolarStyle.text(chip, 14, filter.equals(selectedFilter) ? Color.WHITE : MUTED, true);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(16), dp(12), dp(16), dp(12));
            chip.setMinimumHeight(dp(48));
            chip.setSelected(filter.equals(selectedFilter));
            chip.setBackground(SolarStyle.shape(this, filter.equals(selectedFilter) ? SolarStyle.BLUE : Color.WHITE, 24, 0));
            chip.setFocusable(true);
            chip.setOnClickListener(v -> { selectedFilter = filter; renderFilters(); if (!loading && hasLoaded) renderList(bookings); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.setMarginEnd(dp(8));
            filters.addView(chip, params);
        }
    }

    private void load() {
        if (loading) return;
        setLoading(true);
        final int request = ++generation;
        refreshBtn.setVisibility(View.GONE);
        reservations.history(new ReservationRepository.Callback<List<Reservation>>() {
            @Override public void success(List<Reservation> list) {
                if (isFinishing() || isDestroyed() || request != generation) return;
                setLoading(false);
                hasLoaded = true;
                bookings = list;
                renderList(bookings);
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed() || request != generation) return;
                setLoading(false);
                hasLoaded = false;
                listContainer.removeAllViews();
                TextView error = new TextView(BookingHistoryActivity.this);
                error.setText(text); SolarStyle.notice(error, true);
                listContainer.addView(error);
                refreshBtn.setVisibility(View.VISIBLE);
                if (status == 401) {
                    Toast.makeText(BookingHistoryActivity.this, text, Toast.LENGTH_LONG).show();
                    returnToSignIn();
                }
            }
        });
    }

    private void renderList(List<Reservation> list) {
        listContainer.removeAllViews();
        int shown = 0;
        for (Reservation r : list) {
            boolean upcoming = ("PENDING".equals(r.status) || "APPROVED".equals(r.status));
            try { upcoming &= java.time.OffsetDateTime.parse(r.reservationDate).toInstant().isAfter(java.time.Instant.now()); }
            catch (java.time.format.DateTimeParseException ignored) { /* Keep active bookings with legacy dates visible. */ }
            if (("Upcoming".equals(selectedFilter) && !upcoming)
                    || ("Pending".equals(selectedFilter) && !"PENDING".equals(r.status))
                    || ("Completed".equals(selectedFilter) && !"COMPLETED".equals(r.status))) continue;
            listContainer.addView(card(r)); shown++;
        }
        if (shown == 0) {
            LinearLayout empty = SolarStyle.group(listContainer);
            empty.setPadding(dp(24), dp(32), dp(24), dp(32));
            TextView title = new TextView(this);
            title.setText(list.isEmpty() ? "Your first transfer starts here." : "No " + selectedFilter.toLowerCase(java.util.Locale.ROOT) + " bookings.");
            SolarStyle.text(title, 22, INK, true); empty.addView(title);
            TextView detail = new TextView(this);
            detail.setText(list.isEmpty() ? "Explore a station and choose a slot. Your bookings and transaction QR codes will appear here."
                    : "Choose another filter to see your other transfers.");
            SolarStyle.text(detail, 16, MUTED, false);
            detail.setPadding(0, dp(12), 0, 0); empty.addView(detail);
        }
    }

    private View card(Reservation r) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), SolarStyle.BORDER);
        SolarStyle.card(card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);

        TextView rid = new TextView(this);
        rid.setText(r.stationLabel());
        SolarStyle.text(rid, 24, INK, true);
        rid.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(rid);

        TextView meta = new TextView(this);
        meta.setText(r.stationAddress + "\n" + r.energyAmountKwh + " kWh · " + BookingText.trading(r.tradingType));
        meta.setTextSize(13); meta.setTextColor(MUTED);
        meta.setPadding(0, dp(4), 0, dp(4));
        card.addView(meta);

        TextView date = new TextView(this);
        String dateText = r.reservationDate;
        try {
            dateText = java.time.OffsetDateTime.parse(r.reservationDate)
                    .atZoneSameInstant(java.time.ZoneId.of("Asia/Colombo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", java.util.Locale.getDefault()));
        } catch (java.time.format.DateTimeParseException ignored) { /* Show the server value if a legacy date cannot be formatted. */ }
        date.setText(r.windowLabel() + "\nBooked for " + dateText + " (Sri Lanka)");
        date.setTextSize(13); date.setTextColor(INK);
        card.addView(date);

        TextView status = new TextView(this);
        status.setText(r.status.isEmpty() ? "Unknown" : r.status.substring(0, 1) + r.status.substring(1).toLowerCase(java.util.Locale.ROOT));
        status.setTextSize(13); status.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        status.setTextColor(statusColor(r.status));
        SolarStyle.badge(status, statusColor(r.status));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.topMargin = dp(12); statusParams.bottomMargin = dp(8);
        card.addView(status, statusParams);
        TextView reference = new TextView(this);
        reference.setText("Reference · " + (r.reservationId.isEmpty() ? "Booking" : r.reservationId));
        SolarStyle.text(reference, 14, MUTED, false); card.addView(reference);

        // QR button for APPROVED bookings
        if ("APPROVED".equals(r.status)) {
            Button qrBtn = new Button(this);
            qrBtn.setText("Show Transaction QR");
            qrBtn.setAllCaps(false); qrBtn.setTextSize(13);
            qrBtn.setTextColor(Color.WHITE);
            SolarStyle.button(qrBtn, true);
            qrBtn.setMinHeight(dp(52));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(-1, -2);
            qlp.topMargin = dp(10);
            card.addView(qrBtn, qlp);
            qrBtn.setOnClickListener(v -> {
                android.content.Intent i = new android.content.Intent(this, TransactionQrActivity.class);
                i.putExtra("id", r.id);
                BookingIntents.addDetails(i, r);
                i.putExtra("reservationCode", r.reservationId.isEmpty() ? "Booking" : r.reservationId);
                i.putExtra("stationId", r.stationId);
                i.putExtra("reservationDate", r.reservationDate);
                i.putExtra("energyAmountKwh", r.energyAmountKwh);
                startActivity(i);
            });
        }
        if ("PENDING".equals(r.status) || "APPROVED".equals(r.status)) {
            Button manage = new Button(this);
            manage.setText("Manage booking"); SolarStyle.button(manage, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(10); card.addView(manage, params);
            manage.setOnClickListener(v -> {
                if (loading || cancelling) return;
                new AlertDialog.Builder(this).setTitle("Manage booking")
                        .setItems(new String[]{"Edit booking", "Cancel booking"}, (dialog, which) -> {
                            if (which == 0) editBooking(r); else confirmCancel(r);
                        }).setNegativeButton(R.string.cancel, null).show();
            });
        }
        card.setOnClickListener(v -> {
            android.content.Intent detail = new android.content.Intent(this, BookingSummaryActivity.class);
            BookingIntents.addDetails(detail, r);
            startActivity(detail);
        });
        return card;
    }

    private void editBooking(Reservation r) {
        android.content.Intent intent = new android.content.Intent(this, ModifyBookingActivity.class);
        intent.putExtra("reservationId", r.id);
        BookingIntents.addDetails(intent, r);
        intent.putExtra("stationId", r.stationId);
        intent.putExtra("slotId", r.slotId);
        intent.putExtra("reservationDate", r.reservationDate);
        intent.putExtra("energyAmountKwh", r.energyAmountKwh);
        intent.putExtra("tradingType", r.tradingType);
        startActivity(intent);
    }

    private void confirmCancel(Reservation r) {
        EditText reason = new EditText(this);
        SolarStyle.field(reason);
        reason.setHint(getString(R.string.cancel_reason));
        reason.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_cancel))
                .setMessage(r.stationLabel() + "\n" + r.windowLabel() + "\n" + r.energyAmountKwh + " kWh")
                .setView(reason)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.cancel_booking, (d, w) -> {
                    String reasonText = reason.getText().toString().trim();
                    doCancel(r.id, reasonText);
                }).show();
    }

    private void doCancel(String id, String reason) {
        if (cancelling) return;
        cancelling = true;
        reservations.cancel(id, reason, new ReservationRepository.Callback<Reservation>() {
            @Override public void success(Reservation updated) {
                if (isFinishing() || isDestroyed()) return;
                cancelling = false;
                Toast.makeText(BookingHistoryActivity.this, R.string.booking_cancelled, Toast.LENGTH_SHORT).show();
                load();
            }
            @Override public void failure(int status, String text) {
                if (isFinishing() || isDestroyed()) return;
                cancelling = false;
                Toast.makeText(BookingHistoryActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int statusColor(String status) {
        switch (status) {
            case "PENDING": return AMBER;
            case "APPROVED":
            case "COMPLETED": return GREEN;
            case "CANCELLED": return RED;
            default: return INK;
        }
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        swipeRefresh.setRefreshing(loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        refreshBtn.setEnabled(!loading);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); SolarStyle.text(v, size, color, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        content.addView(v, lp);
        return v;
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
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
