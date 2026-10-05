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
    private final List<String> initialValues = new ArrayList<>();
    private ProfileCache.Snapshot current;
    private boolean busy;
    private String page = "login";
    private boolean accountSelected;
    private int requestGeneration;
    private SolarNavigation.Tab pendingTab;


    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        accounts = ((SolarApplication) getApplication()).accounts();
        accountSelected = "ACCOUNT".equals(getIntent().getStringExtra(SolarNavigation.DESTINATION));
        readPendingTab(getIntent());
        if (state != null) accountSelected = state.getBoolean("accountSelected", accountSelected);
        if (accounts.signedIn()) {
            if (state != null && state.containsKey("profile")) {
                try {
                    current = new ProfileCache.Snapshot(new Profile(new org.json.JSONObject(state.getString("profile"))),
                            state.getBoolean("cached"), state.getLong("fetchedAt"));
                    String restored = state.getString("page", "home");
                    if ("edit".equals(restored)) showEditProfile(current);
                    else if ("deactivate".equals(restored)) showDeactivation();
                    else showDestination(current);
                } catch (org.json.JSONException e) { refresh(); }
            } else refresh();
        } else if (state != null && "register".equals(state.getString("page"))) showRegister();
        else showLogin();
        if (state != null) {
            ArrayList<String> draft = state.getStringArrayList("draft");
            if (draft != null) for (int i = 0; i < Math.min(fields.size(), draft.size()); i++) {
                if ((fields.get(i).getInputType() & InputType.TYPE_MASK_VARIATION) != InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    fields.get(i).setText(draft.get(i));
            }
        }
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        accountSelected = "ACCOUNT".equals(intent.getStringExtra(SolarNavigation.DESTINATION));
        readPendingTab(intent);
        if (accounts.signedIn() && current != null) showDestination(current);
        else if (accounts.signedIn()) refresh();
        else showLogin();
    }

    @Override protected void onResume() {
        super.onResume();
        if (accounts != null && !accounts.signedIn() && current != null) { current = null; showLogin(); }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("page", page);
        state.putBoolean("accountSelected", accountSelected);
        if (current != null && accounts.signedIn()) {
            state.putString("profile", current.profile.json.toString());
            state.putBoolean("cached", current.cached);
            state.putLong("fetchedAt", current.fetchedAt);
        }
        ArrayList<String> draft = new ArrayList<>();
        for (EditText field : fields) draft.add((field.getInputType() & InputType.TYPE_MASK_VARIATION)
                == InputType.TYPE_TEXT_VARIATION_PASSWORD ? "" : value(field));
        state.putStringArrayList("draft", draft);
    }

    @Override protected SolarNavigation.Tab navigationTab() {
        if ("home".equals(page) || "account".equals(page) || "loading".equals(page))
            return accountSelected ? SolarNavigation.Tab.ACCOUNT : SolarNavigation.Tab.HOME;
        return null;
    }
    @Override protected boolean showsBackNavigation() {
        return "register".equals(page) || "edit".equals(page) || "deactivate".equals(page);
    }
    @Override protected void selectTab(SolarNavigation.Tab tab) {
        if (busy) return;
        if (tab == SolarNavigation.Tab.HOME || tab == SolarNavigation.Tab.ACCOUNT) {
            accountSelected = tab == SolarNavigation.Tab.ACCOUNT;
            if (current != null) showDestination(current); else refresh();
        } else super.selectTab(tab);
    }
    @Override protected void navigateBack() {
        if (busy) return;
        if ("register".equals(page) || "edit".equals(page) || "deactivate".equals(page)) {
            Runnable back = "register".equals(page) ? this::showLogin : () -> showProfile(current);
            boolean changed = false;
            for (int i = 0; i < fields.size(); i++) changed |= !value(fields.get(i)).equals(initialValues.get(i));
            if (!changed) { back.run(); return; }
            new AlertDialog.Builder(this).setTitle("Discard changes?")
                    .setMessage("Your unsaved changes will be lost.")
                    .setNegativeButton("Keep editing", null)
                    .setPositiveButton("Discard", (dialog, which) -> back.run()).show();
        } else if (accountSelected && current != null) { accountSelected = false; showHome(current); }
        else super.navigateBack();
    }
    private void showDestination(ProfileCache.Snapshot snapshot) {
        if (accountSelected) showProfile(snapshot); else showHome(snapshot);
        if (pendingTab != null) {
            SolarNavigation.Tab destination = pendingTab;
            pendingTab = null;
            SolarNavigation.open(this, destination);
        }
    }
    private void readPendingTab(android.content.Intent intent) {
        String destination = intent.getStringExtra(SolarNavigation.DESTINATION);
        if ("EXPLORE".equals(destination)) pendingTab = SolarNavigation.Tab.EXPLORE;
        else if ("BOOKINGS".equals(destination)) pendingTab = SolarNavigation.Tab.BOOKINGS;
        else pendingTab = null;
        intent.removeExtra(SolarNavigation.DESTINATION);
    }

    private void showLogin() {
        requestGeneration++;
        pendingTab = null;
        page = "login"; accountSelected = false;
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
        Button operator = button(R.string.grid_operator_link, false, () -> {
            startActivity(new android.content.Intent(this, OperatorLoginActivity.class));
        });
        SolarStyle.link(operator);
    }

    private void showRegister() {
        page = "register"; setTitle(R.string.register_title);
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
    }

    private void showHome(ProfileCache.Snapshot snapshot) {
        current = snapshot; page = "home"; accountSelected = false;
        screen("Home", "A little energy. A brighter community.");
        TextView greeting = text("Welcome back, " + snapshot.profile.fullName.split(" ")[0] + ".", 20, INK, true);
        ((LinearLayout.LayoutParams) greeting.getLayoutParams()).bottomMargin = dp(16);
        if (snapshot.cached) offlineNote(snapshot);

        LinearLayout feature = SolarStyle.group(content);
        feature.setPadding(dp(24), dp(24), dp(24), dp(24));
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(R.drawable.ic_nav_bolt);
        feature.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView heading = new TextView(this);
        heading.setText("Put your energy to work.");
        SolarStyle.text(heading, 28, INK, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(16); headingParams.bottomMargin = dp(8);
        feature.addView(heading, headingParams);
        TextView detail = new TextView(this);
        detail.setText("Find a nearby microgrid, choose an available slot, and book your next energy transfer.");
        SolarStyle.text(detail, 17, MUTED, false);
        feature.addView(detail);
        Button explore = new Button(this);
        explore.setText("Explore stations"); SolarStyle.button(explore, true);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, -2);
        buttonParams.topMargin = dp(24);
        feature.addView(explore, buttonParams);
        explore.setOnClickListener(v -> selectTab(SolarNavigation.Tab.EXPLORE));

        SolarStyle.section(content, "Your next step");
        LinearLayout group = SolarStyle.group(content);
        SolarStyle.row(group, R.drawable.ic_nav_bookings, "Make a booking", "Choose a station and an energy slot",
                () -> startActivity(new android.content.Intent(this, CreateBookingActivity.class)));
        SolarStyle.row(group, R.drawable.ic_nav_home, "View your bookings", "Upcoming transfers, history, and QR codes",
                () -> selectTab(SolarNavigation.Tab.BOOKINGS));
        text("Station availability is checked live when you book.", 14, MUTED, false);
    }

    private void showProfile(ProfileCache.Snapshot snapshot) {
        current = snapshot; page = "account"; accountSelected = true;
        Profile profile = snapshot.profile;
        screen("Account", "Your details, all in one place.");
        LinearLayout identity = SolarStyle.group(content);
        identity.setPadding(dp(20), dp(24), dp(20), dp(24));
        android.widget.ImageView avatar = new android.widget.ImageView(this);
        avatar.setImageResource(R.drawable.ic_account_avatar);
        identity.addView(avatar, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView name = new TextView(this);
        name.setText(profile.fullName); SolarStyle.text(name, 24, INK, true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(16); nameParams.bottomMargin = dp(8);
        identity.addView(name, nameParams);
        TextView nic = new TextView(this);
        nic.setText(getString(R.string.nic_value, profile.nic)); SolarStyle.text(nic, 14, MUTED, false);
        identity.addView(nic);
        TextView status = new TextView(this);
        status.setText(profile.status); SolarStyle.badge(status, GREEN);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.topMargin = dp(12);
        identity.addView(status, statusParams);
        if (snapshot.cached) offlineNote(snapshot);
        SolarStyle.section(content, "Personal information");
        LinearLayout details = SolarStyle.group(content);
        SolarStyle.row(details, R.drawable.ic_nav_account, "Email", profile.email, null);
        SolarStyle.row(details, R.drawable.ic_nav_account, "Phone", profile.phone, null);
        SolarStyle.row(details, R.drawable.ic_nav_home, "Address", profile.address, null);
        LinearLayout settings = SolarStyle.group(content);
        SolarStyle.row(settings, R.drawable.ic_nav_account, "Edit personal details", "Name and contact information", () -> showEditProfile(snapshot));
        SolarStyle.row(settings, R.drawable.ic_nav_explore, "Refresh account", "Get the latest account information", this::refresh);
        if (profile.requestStatus != null) note(getString(R.string.last_request, profile.requestStatus));
        if (profile.decisionNote != null) text(profile.decisionNote, 14, MUTED, false);
        if (!"Pending".equals(profile.requestStatus)) {
            View deactivate = SolarStyle.row(settings, R.drawable.ic_nav_account, "Request deactivation", "Submit a request for review", this::showDeactivation);
            deactivate.setEnabled(!snapshot.cached);
            deactivate.setAlpha(snapshot.cached ? 0.5f : 1f);
        }
        SolarStyle.danger(button(R.string.sign_out, false, this::confirmLogout));
    }

    private void offlineNote(ProfileCache.Snapshot snapshot) {
        String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(snapshot.fetchedAt));
        note(getString(R.string.offline, date));
    }

    private void showEditProfile(ProfileCache.Snapshot snapshot) {
        if (busy) return;
        current = snapshot;
        Profile profile = snapshot.profile;
        page = "edit"; setTitle("Edit details");
        screen("Edit details", "Keep your contact information up to date.");
        if (snapshot.cached) offlineNote(snapshot);

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
        button(R.string.cancel, false, this::navigateBack);
    }

    private void showDeactivation() {
        if (busy) return;
        if (current == null || current.cached) { showMessage(getString(R.string.offline_writes), true); return; }
        page = "deactivate"; setTitle(R.string.request_title);
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
        button(R.string.cancel, false, this::navigateBack);
    }

    private void refresh() {
        if (busy) return;
        page = "loading";
        screen(accountSelected ? "Account" : "Home", "Getting your account ready…");
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
        final int generation = ++requestGeneration;
        return new AccountRepository.Callback<>() {
            @Override public void success(ProfileCache.Snapshot snapshot) {
                if (!alive() || generation != requestGeneration) return;
                accountSelected = false;
                showHome(snapshot);
            }
            @Override public void failure(int status, String text) {
                if (generation == requestGeneration) handleFailure(status, text);
            }
        };
    }

    private AccountRepository.Callback<ProfileCache.Snapshot> profileCallback(int successMessage) {
        final int generation = ++requestGeneration;
        return new AccountRepository.Callback<>() {
            @Override public void success(ProfileCache.Snapshot snapshot) {
                if (!alive() || generation != requestGeneration) return;
                if (successMessage != 0) showProfile(snapshot); else showDestination(snapshot);
                if (successMessage != 0) showMessage(getString(successMessage), false);
            }
            @Override public void failure(int status, String text) {
                if (generation == requestGeneration) handleFailure(status, text);
            }
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
    private void screen(int title, int introduction) { screen(getString(title), getString(introduction)); }

    private void screen(String title, String introduction) {
        busy = false; actions.clear(); fields.clear(); initialValues.clear();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        scroll.requestApplyInsets();
        if ("login".equals(page)) {
            TextView brand = text(getString(R.string.brand), 16, INK, true);
            SolarStyle.brand(brand);
        }
        SolarStyle.hero(content, title, introduction);
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
        initialValues.add(initial);
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
        if (value) {
            android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(content.getWindowToken(), 0);
        }
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        for (Button action : actions) action.setEnabled(!value && Boolean.TRUE.equals(action.getTag()));
        for (EditText field : fields) field.setEnabled(!value);
        if (value) showMessage(getString(R.string.working), false);
    }
    private void showMessage(String value, boolean error) {
        message.setText(value); SolarStyle.notice(message, error); message.setVisibility(View.VISIBLE);
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
