package lk.solar.microgrid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.AccountRepository;
import lk.solar.microgrid.data.Profile;
import lk.solar.microgrid.data.ProfileCache;

/** Native Android widgets. This activity manages presentation only; the API owns account rules. */
public final class MainActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;
    private AccountRepository accounts;
    private LinearLayout content;
    private TextView message;
    private ProgressBar progress;
    private final List<Button> actions = new ArrayList<>();
    private final List<EditText> fields = new ArrayList<>();
    private ProfileCache.Snapshot current;
    private boolean busy;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        accounts = ((SolarApplication) getApplication()).accounts();
        if (accounts.signedIn()) refresh(); else showLogin();
    }

    private void showLogin() {
        screen(R.string.welcome, R.string.login_intro);
        EditText nic = field(R.string.nic, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 12);
        nic.setHint(R.string.nic_hint);
        nic.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
        EditText password = passwordField();
        button(R.string.sign_in, true, () -> {
            if (!required(nic, password)) return;
            setBusy(true);
            accounts.login(value(nic), value(password), signInCallback());
        });
        button(R.string.register_link, false, this::showRegister);
        button(R.string.grid_operator_link, false, () -> {
            startActivity(new android.content.Intent(this, OperatorLoginActivity.class));
        });
    }

    private void showRegister() {
        screen(R.string.register_title, R.string.register_intro);
        EditText nic = field(R.string.nic, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 12);
        nic.setHint(R.string.nic_hint);
        EditText name = field(R.string.full_name, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, 100);
        EditText email = field(R.string.email, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 254);
        EditText phone = field(R.string.phone, "", InputType.TYPE_CLASS_PHONE, 20);
        EditText address = field(R.string.address, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 300);
        EditText password = passwordField();
        text(getString(R.string.password_help), 12, MUTED, false);
        button(R.string.create_account, true, () -> {
            if (!required(nic, name, email, phone, address, password)) return;
            if (value(password).length() < 12) { showMessage(getString(R.string.password_help), true); return; }
            setBusy(true);
            accounts.register(value(nic), value(name), value(email), value(phone), value(address), value(password), signInCallback());
        });
        button(R.string.back_sign_in, false, this::showLogin);
    }

    private void showProfile(ProfileCache.Snapshot snapshot) {
        current = snapshot;
        Profile profile = snapshot.profile;
        screen(R.string.profile_title, R.string.profile_intro);

        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{SolarStyle.SKY, Color.WHITE});
        bg.setCornerRadius(dp(12));
        SolarStyle.card(profileCard);
        profileCard.setElevation(0);
        profileCard.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView nameView = new TextView(this);
        nameView.setText(profile.fullName);
        nameView.setTextSize(24); nameView.setTextColor(INK); nameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        profileCard.addView(nameView);

        TextView nicView = new TextView(this);
        nicView.setText(getString(R.string.nic_value, profile.nic));
        nicView.setTextSize(13); nicView.setTextColor(MUTED);
        profileCard.addView(nicView);

        TextView statusView = new TextView(this);
        statusView.setText(getString(R.string.status_value, profile.status));
        statusView.setTextSize(13); statusView.setTextColor(GREEN); statusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.topMargin = dp(8);
        profileCard.addView(statusView, sp);

        LinearLayout.LayoutParams pcp = new LinearLayout.LayoutParams(-1, -2);
        pcp.bottomMargin = dp(20);
        content.addView(profileCard, pcp);

        if (snapshot.cached) {
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(snapshot.fetchedAt));
            note(getString(R.string.offline, date));
        }

        TextView actionsTitle = new TextView(this);
        actionsTitle.setText("Quick Actions");
        actionsTitle.setTextSize(16); actionsTitle.setTextColor(INK); actionsTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams atp = new LinearLayout.LayoutParams(-1, -2);
        atp.topMargin = dp(10); atp.bottomMargin = dp(10);
        content.addView(actionsTitle, atp);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        content.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        grid.addView(row1, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        grid.addView(row2, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        grid.addView(row3, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row4 = new LinearLayout(this);
        row4.setOrientation(LinearLayout.HORIZONTAL);
        grid.addView(row4, new LinearLayout.LayoutParams(-1, -2));

        row1.addView(createActionCard("Explore Stations", () -> startActivity(new android.content.Intent(this, StationMapActivity.class))));
        row1.addView(createActionCard("Create Booking", () -> startActivity(new android.content.Intent(this, CreateBookingActivity.class))));

        row2.addView(createActionCard("My Bookings", () -> startActivity(new android.content.Intent(this, BookingHistoryActivity.class))));
        row2.addView(createActionCard("Pending Bookings", () -> startActivity(new android.content.Intent(this, PendingBookingsActivity.class))));

        row3.addView(createActionCard("Search Bookings", () -> startActivity(new android.content.Intent(this, SearchBookingActivity.class))));
        row3.addView(createActionCard("Refresh", this::refresh));

        row4.addView(createActionCard("Edit Profile", () -> showEditProfile(snapshot)));
        row4.addView(createActionCard("Sign Out", this::confirmLogout));
    }

    private void showEditProfile(ProfileCache.Snapshot snapshot) {
        current = snapshot;
        Profile profile = snapshot.profile;
        screen(R.string.profile_title, R.string.profile_intro);

        TextView formTitle = new TextView(this);
        formTitle.setText("Update Details");
        formTitle.setTextSize(24); formTitle.setTextColor(INK); formTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams ftp = new LinearLayout.LayoutParams(-1, -2);
        ftp.topMargin = dp(20);
        content.addView(formTitle, ftp);

        EditText name = field(R.string.full_name, profile.fullName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, 100);
        EditText email = field(R.string.email, profile.email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 254);
        EditText phone = field(R.string.phone, profile.phone, InputType.TYPE_CLASS_PHONE, 20);
        EditText address = field(R.string.address, profile.address, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 300);
        Button save = button(R.string.save_profile, true, () -> {
            if (!required(name, email, phone, address)) return;
            setBusy(true);
            accounts.save(value(name), value(email), value(phone), value(address), profile.version, profileCallback(R.string.profile_saved));
        });
        allow(save, !snapshot.cached);
        if (profile.requestStatus != null) {
            note(getString(R.string.last_request, profile.requestStatus));
            if (profile.decisionNote != null) text(profile.decisionNote, 13, MUTED, false);
        }
        if ("Pending".equals(profile.requestStatus)) note(getString(R.string.request_pending));
        else {
            Button request = button(R.string.request_deactivation, false, this::showDeactivation);
            allow(request, !snapshot.cached);
        }

        button(R.string.back_account, false, () -> showProfile(current));
    }

    private void showDeactivation() {
        if (current == null || current.cached) { showMessage(getString(R.string.offline_writes), true); return; }
        screen(R.string.request_title, R.string.request_intro);
        EditText reason = field(R.string.reason, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 500);
        reason.setMinLines(4);
        button(R.string.submit_request, true, () -> {
            String reasonText = value(reason).trim();
            if (reasonText.length() < 5) { showMessage(getString(R.string.reason_help), true); return; }
            new AlertDialog.Builder(this).setMessage(R.string.confirm_request)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.submit_request, (dialog, which) -> {
                    setBusy(true);
                    accounts.deactivate(reasonText, current.profile.version, profileCallback(R.string.request_sent));
                }).show();
        });
        button(R.string.back_account, false, () -> showProfile(current));
    }

    private void refresh() {
        screen(R.string.profile_title, R.string.profile_intro);
        button(R.string.refresh, false, this::refresh);
        button(R.string.sign_out, false, this::confirmLogout);
        setBusy(true);
        accounts.refresh(profileCallback(0));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this).setTitle(R.string.sign_out).setMessage(R.string.confirm_sign_out)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.leave_device, (dialog, which) -> logout(false))
            .setPositiveButton(R.string.sign_out_devices, (dialog, which) -> logout(true)).show();
    }
    private void logout(boolean allDevices) {
        setBusy(true);
        accounts.logout(allDevices, new AccountRepository.Callback<>() {
            @Override public void success(Void ignored) { if (alive()) { current = null; showLogin(); } }
            @Override public void failure(int status, String text) { handleFailure(status, text); }
        });
    }

    private AccountRepository.Callback<ProfileCache.Snapshot> signInCallback() {
        return new AccountRepository.Callback<>() {
            @Override public void success(ProfileCache.Snapshot snapshot) {
                if (!alive()) return;
                // Keep the account page underneath the map for My account and Back.
                showProfile(snapshot);
                startActivity(new android.content.Intent(MainActivity.this, StationMapActivity.class));
            }
            @Override public void failure(int status, String text) { handleFailure(status, text); }
        };
    }

    private AccountRepository.Callback<ProfileCache.Snapshot> profileCallback(int successMessage) {
        return new AccountRepository.Callback<>() {
            @Override public void success(ProfileCache.Snapshot snapshot) {
                if (!alive()) return;
                showProfile(snapshot);
                if (successMessage != 0) showMessage(getString(successMessage), false);
            }
            @Override public void failure(int status, String text) { handleFailure(status, text); }
        };
    }
    private void handleFailure(int status, String text) {
        if (!alive()) return;
        setBusy(false);
        if (status == 401) { current = null; showLogin(); }
        showMessage(text, true);
        if (status == 409 && accounts.signedIn()) button(R.string.refresh, false, this::refresh);
    }
    private boolean alive() { return !isFinishing() && !isDestroyed(); }

    @SuppressWarnings("deprecation")
    private void screen(int title, int introduction) {
        busy = false; actions.clear(); fields.clear();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(36));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        scroll.requestApplyInsets();
        SolarStyle.hero(content, getString(title), getString(introduction));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        message = text("", 13, INK, false);
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        message.setVisibility(View.GONE);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); SolarStyle.text(view, size, color, bold);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
        content.addView(view, layout);
        return view;
    }
    private void note(String value) {
        TextView view = text(value, 13, INK, false);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(shape(SolarStyle.MINT, 0));
    }
    private EditText field(int label, String initial, int inputType, int maxLength) {
        TextView caption = text(getString(label), 12, INK, true);
        ((LinearLayout.LayoutParams) caption.getLayoutParams()).topMargin = dp(18);
        EditText editor = new EditText(this);
        editor.setId(View.generateViewId()); caption.setLabelFor(editor.getId());
        editor.setTextSize(15); editor.setTextColor(INK); editor.setHintTextColor(MUTED);
        editor.setInputType(inputType); editor.setText(initial);
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        editor.setBackground(shape(Color.WHITE, SolarStyle.BORDER));
        editor.setPadding(dp(13), dp(12), dp(13), dp(12));
        SolarStyle.field(editor);
        content.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        fields.add(editor);
        return editor;
    }
    private EditText passwordField() {
        EditText field = field(R.string.password, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 128);
        field.setSaveEnabled(false);
        field.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
        return field;
    }
    private Button button(int label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(13); button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : GREEN);
        SolarStyle.button(button, primary);
        button.setMinHeight(dp(52)); button.setTag(true);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(15);
        content.addView(button, layout); actions.add(button);
        button.setOnClickListener(view -> { if (!busy) action.run(); });
        return button;
    }
    private void allow(Button button, boolean allowed) { button.setTag(allowed); button.setEnabled(allowed && !busy); }
    private void setBusy(boolean value) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        for (Button action : actions) action.setEnabled(!value && Boolean.TRUE.equals(action.getTag()));
        for (EditText field : fields) field.setEnabled(!value);
        if (value) showMessage(getString(R.string.working), false);
    }
    private void showMessage(String value, boolean error) {
        message.setText(value); message.setTextColor(error ? SolarStyle.RED : GREEN);
        message.setPadding(0, dp(14), 0, dp(10)); message.setVisibility(View.VISIBLE);
    }
    private boolean required(EditText... inputs) {
        for (EditText input : inputs) if (value(input).trim().isEmpty()) {
            input.setError(getString(R.string.required_fields)); input.requestFocus(); return false;
        }
        return true;
    }
    private static String value(EditText input) { return input.getText().toString(); }
    private View createActionCard(String title, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(android.view.Gravity.CENTER);
        SolarStyle.card(card);
        card.setElevation(0);
        card.setPadding(dp(10), dp(24), dp(10), dp(24));
        card.setClickable(true);
        card.setOnClickListener(v -> { if (!busy) action.run(); });

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(13);
        titleView.setTextColor(GREEN);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setGravity(android.view.Gravity.CENTER);
        card.addView(titleView);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1.0f);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(params);
        return card;
    }

    private GradientDrawable shape(int fill, int stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
