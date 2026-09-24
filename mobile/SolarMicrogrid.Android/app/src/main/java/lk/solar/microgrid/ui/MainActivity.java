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
public final class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);
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
            accounts.login(value(nic), value(password), profileCallback(0));
        });
        button(R.string.register_link, false, this::showRegister);
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
            accounts.register(value(nic), value(name), value(email), value(phone), value(address), value(password), profileCallback(0));
        });
        button(R.string.back_sign_in, false, this::showLogin);
    }

    private void showProfile(ProfileCache.Snapshot snapshot) {
        current = snapshot;
        Profile profile = snapshot.profile;
        screen(R.string.profile_title, R.string.profile_intro);
        text(profile.fullName, 24, INK, true);
        text(getString(R.string.nic_value, profile.nic), 13, MUTED, false);
        text(getString(R.string.status_value, profile.status), 13, GREEN, true);
        if (snapshot.cached) {
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(snapshot.fetchedAt));
            note(getString(R.string.offline, date));
        }
        button(R.string.refresh, false, this::refresh);
        button(R.string.nearby_stations, true, () -> startActivity(new android.content.Intent(this, StationMapActivity.class)));

        // ===== Sajith: Reservation features =====
        button(R.string.create_booking, true, () -> startActivity(new android.content.Intent(this, CreateBookingActivity.class)));
        button(R.string.my_bookings, false, () -> startActivity(new android.content.Intent(this, BookingHistoryActivity.class)));
        button(R.string.pending_bookings, false, () -> startActivity(new android.content.Intent(this, PendingBookingsActivity.class)));

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
        button(R.string.sign_out, false, this::confirmLogout);
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
        scroll.setBackgroundColor(Color.rgb(245, 247, 243));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(26), dp(28), dp(26), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();
        TextView brand = text(getString(R.string.brand), 12, GREEN, true);
        brand.setLetterSpacing(0.13f);
        TextView heading = text(getString(title), 32, INK, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(12);
        text(getString(introduction), 14, MUTED, false);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        message = text("", 13, INK, false);
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        message.setVisibility(View.GONE);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
        content.addView(view, layout);
        return view;
    }
    private void note(String value) {
        TextView view = text(value, 13, INK, false);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackground(shape(Color.rgb(233, 240, 221), 0));
    }
    private EditText field(int label, String initial, int inputType, int maxLength) {
        TextView caption = text(getString(label), 12, INK, true);
        ((LinearLayout.LayoutParams) caption.getLayoutParams()).topMargin = dp(18);
        EditText editor = new EditText(this);
        editor.setId(View.generateViewId()); caption.setLabelFor(editor.getId());
        editor.setTextSize(15); editor.setTextColor(INK); editor.setHintTextColor(MUTED);
        editor.setInputType(inputType); editor.setText(initial);
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        editor.setBackground(shape(Color.WHITE, Color.rgb(213, 224, 214)));
        editor.setPadding(dp(13), dp(12), dp(13), dp(12));
        editor.setMinimumHeight(dp(50));
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
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(234, 240, 227)));
        button.setMinHeight(dp(50)); button.setTag(true);
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
        message.setText(value); message.setTextColor(error ? Color.rgb(155, 66, 44) : GREEN);
        message.setPadding(0, dp(14), 0, dp(10)); message.setVisibility(View.VISIBLE);
    }
    private boolean required(EditText... inputs) {
        for (EditText input : inputs) if (value(input).trim().isEmpty()) {
            input.setError(getString(R.string.required_fields)); input.requestFocus(); return false;
        }
        return true;
    }
    private static String value(EditText input) { return input.getText().toString(); }
    private GradientDrawable shape(int fill, int stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}