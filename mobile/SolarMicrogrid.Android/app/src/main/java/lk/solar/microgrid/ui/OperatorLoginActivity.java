/*
 * File: OperatorLoginActivity.java
 * Author: Lakshman K A P C
 * Description: Android activity for grid operator login.
 */
package lk.solar.microgrid.ui;

import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.OperatorRepository;

public class OperatorLoginActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;
    private OperatorRepository operators;
    private LinearLayout content;
    private TextView message;
    private ProgressBar progress;
    private final List<Button> actions = new ArrayList<>();
    private final List<EditText> fields = new ArrayList<>();
    private boolean busy;

    @Override public void onCreate(Bundle state) {
        // Initialize the activity and check for existing operator session
        super.onCreate(state);
        operators = ((SolarApplication) getApplication()).operators();
        if (operators.signedIn()) {
            startActivity(new Intent(this, OperatorDashboardActivity.class));
            finish();
            return;
        }
        showLogin();
    }

    private void showLogin() {
        // Display the operator login screen
        screen(R.string.operator_login_title, R.string.operator_login_intro);
        EditText username = field(R.string.operator_username, "", InputType.TYPE_CLASS_TEXT, 50);
        EditText password = passwordField();

        button(R.string.sign_in, true, () -> {
            if (!required(username, password)) return;
            setBusy(true);
            operators.login(value(username), value(password), new OperatorRepository.Callback<Void>() {
                @Override public void success(Void result) {
                    if (!alive()) return;
                    startActivity(new Intent(OperatorLoginActivity.this, OperatorDashboardActivity.class));
                    finish();
                }
                @Override public void failure(int status, String text) {
                    if (!alive()) return;
                    setBusy(false);
                    showMessage(text, true);
                }
            });
        });

        button(R.string.back_to_prosumer_login, false, () -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
    }

    private boolean alive() { 
        // Check if the activity is still valid
        return !isFinishing() && !isDestroyed(); 
    }

    @SuppressWarnings("deprecation")
    private void screen(int title, int introduction) {
        // Setup the base screen layout
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
        // Create a text view with specified formatting
        TextView view = new TextView(this);
        view.setText(value); SolarStyle.text(view, size, color, bold);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
        content.addView(view, layout);
        return view;
    }

    private EditText field(int label, String initial, int inputType, int maxLength) {
        // Create a text input field
        TextView caption = text(getString(label), 12, INK, true);
        ((LinearLayout.LayoutParams) caption.getLayoutParams()).topMargin = dp(18);
        EditText editor = new EditText(this);
        editor.setId(View.generateViewId()); caption.setLabelFor(editor.getId());
        editor.setTextSize(15); editor.setTextColor(INK); editor.setHintTextColor(MUTED);
        editor.setInputType(inputType); editor.setText(initial);
        editor.setBackground(shape(Color.WHITE, SolarStyle.BORDER));
        editor.setPadding(dp(13), dp(12), dp(13), dp(12));
        SolarStyle.field(editor);
        content.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        fields.add(editor);
        return editor;
    }

    private EditText passwordField() {
        // Create a password input field
        EditText field = field(R.string.password, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 128);
        field.setSaveEnabled(false);
        field.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
        return field;
    }

    private Button button(int label, boolean primary, Runnable action) {
        // Create an interactive button
        Button button = new Button(this);
        button.setText(label); button.setTextSize(13); button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? Color.WHITE : GREEN);
        SolarStyle.button(button, primary);
        if (primary) button.setElevation(0);
        button.setMinHeight(dp(52)); button.setTag(true);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(15);
        content.addView(button, layout); actions.add(button);
        button.setOnClickListener(view -> { if (!busy) action.run(); });
        return button;
    }

    private void setBusy(boolean value) {
        // Toggle the loading state of the UI
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        for (Button action : actions) action.setEnabled(!value && Boolean.TRUE.equals(action.getTag()));
        for (EditText field : fields) field.setEnabled(!value);
        if (value) showMessage("Signing in...", false);
    }

    private void showMessage(String value, boolean error) {
        // Display a message to the user
        message.setText(value); message.setTextColor(error ? SolarStyle.RED : GREEN);
        message.setPadding(0, dp(14), 0, dp(10)); message.setVisibility(View.VISIBLE);
    }

    private boolean required(EditText... inputs) {
        // Validate required fields
        for (EditText input : inputs) if (value(input).trim().isEmpty()) {
            input.setError(getString(R.string.required_fields)); input.requestFocus(); return false;
        }
        return true;
    }

    private static String value(EditText input) { 
        // Get the value of a text input
        return input.getText().toString(); 
    }

    private GradientDrawable shape(int fill, int stroke) {
        // Create a background shape drawable
        GradientDrawable shape = new GradientDrawable(); shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) { 
        // Convert dp units to pixels
        return Math.round(value * getResources().getDisplayMetrics().density); 
    }
}
