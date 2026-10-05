package lk.solar.microgrid.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.Gravity;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;
import lk.solar.microgrid.R;

/** Consistent native system bars and safe areas for every app screen. */
public abstract class SolarActivity extends Activity {
    @SuppressWarnings("deprecation")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Create the decor before accessing the window's insets controller. Some
        // devices dereference an uninitialized decor inside getInsetsController().
        View decor = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            int appearance = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 27) appearance |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(appearance);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(SolarStyle.BACKGROUND);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::navigateBack);
        }
    }

    protected SolarNavigation.Tab navigationTab() {
        if (this instanceof StationMapActivity) return SolarNavigation.Tab.EXPLORE;
        if (this instanceof BookingHistoryActivity) return SolarNavigation.Tab.BOOKINGS;
        if (this instanceof OperatorDashboardActivity) return SolarNavigation.Tab.OVERVIEW;
        if (this instanceof CompletedOperationsActivity) return SolarNavigation.Tab.COMPLETED;
        if (this instanceof OperatorProfileActivity) return SolarNavigation.Tab.OPERATOR_ACCOUNT;
        return null;
    }

    protected boolean showsBackNavigation() { return navigationTab() == null; }
    protected void navigateBack() { finish(); }
    // API 33+ uses the native OnBackInvokedDispatcher registered above. This is the API 26–32 fallback.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() { navigateBack(); }
    protected void selectTab(SolarNavigation.Tab tab) { SolarNavigation.open(this, tab); }

    protected void returnToSignIn() {
        // A server-revoked reservation session must also clear the account's local session.
        ((lk.solar.microgrid.SolarApplication) getApplication()).accounts().logout(false,
                new lk.solar.microgrid.data.AccountRepository.Callback<Void>() {
                    @Override public void success(Void ignored) { openLogin(); }
                    @Override public void failure(int status, String message) { openLogin(); }
                    private void openLogin() {
                        if (isFinishing() || isDestroyed()) return;
                        startActivity(new android.content.Intent(SolarActivity.this, MainActivity.class)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP));
                        finish();
                    }
                });
    }

    private View backBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(SolarStyle.dp(this, 12), 0, SolarStyle.dp(this, 16), 0);
        bar.setBackgroundColor(SolarStyle.BACKGROUND);
        TextView back = new TextView(this);
        back.setText(R.string.nav_back);
        SolarStyle.text(back, 17, SolarStyle.BLUE, false);
        android.graphics.drawable.Drawable icon = getDrawable(R.drawable.ic_nav_back);
        if (icon != null) icon.setBounds(0, 0, SolarStyle.dp(this, 20), SolarStyle.dp(this, 20));
        back.setCompoundDrawablesRelative(icon, null, null, null);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(SolarStyle.dp(this, 4), SolarStyle.dp(this, 12), SolarStyle.dp(this, 12), SolarStyle.dp(this, 12));
        back.setMinimumHeight(SolarStyle.dp(this, 48));
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(SolarStyle.RIPPLE), null, null));
        back.setFocusable(true);
        back.setOnClickListener(v -> navigateBack());
        bar.addView(back, new LinearLayout.LayoutParams(-2, -2));
        TextView title = new TextView(this);
        title.setText(getTitle());
        SolarStyle.text(title, 15, SolarStyle.INK, true);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setGravity(Gravity.END);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        return bar;
    }

    @SuppressWarnings("deprecation")
    @Override public void setContentView(View root) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(SolarStyle.BACKGROUND);
        if (showsBackNavigation()) shell.addView(backBar(), new LinearLayout.LayoutParams(-1, -2));
        shell.addView(root, new LinearLayout.LayoutParams(-1, 0, 1));
        SolarNavigation.Tab tab = navigationTab();
        View tabBar = tab == null ? null : SolarNavigation.bar(this, tab);
        getWindow().setNavigationBarColor(tab == null ? SolarStyle.BACKGROUND : Color.WHITE);
        if (tabBar != null) shell.addView(tabBar, new LinearLayout.LayoutParams(-1, -2));
        shell.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() |
                        WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                boolean keyboardVisible = insets.isVisible(WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, tabBar != null && !keyboardVisible ? 0 : safe.bottom);
                if (tabBar != null) {
                    tabBar.setVisibility(keyboardVisible ? View.GONE : View.VISIBLE);
                    tabBar.setPadding(0, 0, 0, safe.bottom);
                }
                return WindowInsets.CONSUMED;
            } else {
                boolean keyboardVisible = insets.getSystemWindowInsetBottom() > SolarStyle.dp(this, 150);
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), tabBar != null && !keyboardVisible ? 0 : insets.getSystemWindowInsetBottom());
                if (tabBar != null) {
                    tabBar.setVisibility(keyboardVisible ? View.GONE : View.VISIBLE);
                    tabBar.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                }
            }
            // Insets belong to the shell; map/camera children must not apply them again.
            return insets.consumeSystemWindowInsets();
        });
        super.setContentView(shell);
        shell.requestApplyInsets();
    }
}
