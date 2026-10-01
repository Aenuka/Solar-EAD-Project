package lk.solar.microgrid.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import lk.solar.microgrid.R;

/** Shared native styling, matching the web portal's sky-blue and energy-green palette. */
public final class SolarStyle {
    public static final int BLUE = Color.rgb(0, 113, 197);
    public static final int GREEN = Color.rgb(4, 120, 87);
    public static final int INK = Color.rgb(23, 43, 56);
    public static final int MUTED = Color.rgb(100, 116, 139);
    public static final int BACKGROUND = Color.rgb(245, 247, 249);
    public static final int BORDER = Color.rgb(226, 232, 240);
    public static final int SKY = Color.rgb(235, 246, 255);
    public static final int MINT = Color.rgb(236, 253, 245);
    public static final int RED = Color.rgb(185, 28, 28);
    public static final int ROSE = Color.rgb(254, 242, 242);
    public static final int AMBER = Color.rgb(146, 64, 14);
    public static final int RIPPLE = Color.argb(30, 0, 113, 197);

    private SolarStyle() { }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable shape(Context context, int fill, int radius, int border) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(fill);
        result.setCornerRadius(dp(context, radius));
        if (border != 0) result.setStroke(dp(context, 1), border);
        return result;
    }

    public static void text(TextView view, int size, int color, boolean emphasis) {
        view.setTextSize(Math.max(13, size));
        view.setTextColor(color);
        view.setTypeface(Typeface.create(emphasis ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setLineSpacing(dp(view.getContext(), 2), 1.05f);
        if (size >= 24) {
            view.setLetterSpacing(-0.025f);
            if (Build.VERSION.SDK_INT >= 28) view.setAccessibilityHeading(true);
        }
    }

    public static void brand(TextView view) {
        text(view, 14, INK, true);
        view.setLetterSpacing(0);
        android.graphics.drawable.Drawable icon = view.getContext().getDrawable(R.drawable.ic_solar);
        if (icon != null) icon.setBounds(0, 0, dp(view.getContext(), 28), dp(view.getContext(), 28));
        view.setCompoundDrawablesRelative(icon, null, null, null);
        view.setCompoundDrawablePadding(dp(view.getContext(), 10));
    }

    public static void button(Button view, boolean primary) {
        buttonColors(view, primary ? BLUE : SKY, primary ? Color.WHITE : BLUE);
    }

    public static void danger(Button view) { buttonColors(view, ROSE, RED); }

    private static void buttonColors(Button view, int fill, int foreground) {
        Context context = view.getContext();
        view.setAllCaps(false);
        text(view, 16, foreground, true);
        view.setBackgroundTintList(null);
        StateListDrawable backgrounds = new StateListDrawable();
        backgrounds.addState(new int[]{-android.R.attr.state_enabled}, shape(context, BORDER, 28, 0));
        backgrounds.addState(new int[]{}, shape(context, fill, 28, 0));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE), backgrounds,
                shape(context, Color.WHITE, 28, 0)));
        view.setTextColor(new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}},
                new int[]{MUTED, foreground}));
        view.setStateListAnimator(null);
        view.setElevation(0);
        view.setMinimumHeight(dp(context, 52));
        view.setMinHeight(dp(context, 52));
        view.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 12));
    }

    public static void field(EditText view) {
        Context context = view.getContext();
        view.setTextSize(16);
        view.setTextColor(INK);
        view.setHintTextColor(MUTED);
        view.setBackgroundTintList(null);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, shape(context, Color.WHITE, 14, BLUE));
        background.addState(new int[]{-android.R.attr.state_enabled}, shape(context, BACKGROUND, 14, BORDER));
        background.addState(new int[]{}, shape(context, Color.WHITE, 14, BORDER));
        view.setBackground(background);
        view.setMinimumHeight(dp(context, 54));
        view.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
    }

    public static void card(View view) {
        view.setBackground(shape(view.getContext(), Color.WHITE, 24, BORDER));
        view.setElevation(0);
    }

    public static void interactiveCard(View view) {
        Context context = view.getContext();
        view.setBackgroundTintList(null);
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE),
                shape(context, Color.WHITE, 24, BORDER), shape(context, Color.WHITE, 24, 0)));
        view.setElevation(0);
        view.setFocusable(true);
        view.setMinimumHeight(dp(context, 64));
    }

    public static void badge(TextView view, int color) {
        text(view, 13, color, true);
        int fill = color == GREEN ? MINT : color == RED ? ROSE : color == AMBER ? Color.rgb(255, 251, 235) : SKY;
        view.setBackground(shape(view.getContext(), fill, 16, 0));
        view.setPadding(dp(view.getContext(), 10), dp(view.getContext(), 6),
                dp(view.getContext(), 10), dp(view.getContext(), 6));
    }

    public static void notice(TextView view, boolean error) {
        text(view, 14, error ? RED : GREEN, false);
        view.setBackground(shape(view.getContext(), error ? ROSE : MINT, 16, 0));
        view.setPadding(dp(view.getContext(), 16), dp(view.getContext(), 14),
                dp(view.getContext(), 16), dp(view.getContext(), 14));
        view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    public static void hero(LinearLayout parent, String title, String introduction) {
        Context context = parent.getContext();
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 28));
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{SKY, MINT});
        gradient.setCornerRadius(dp(context, 28));
        panel.setBackground(gradient);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(context, 12);
        parent.addView(panel, params);
        TextView brand = new TextView(context);
        brand.setText(R.string.brand);
        brand(brand);
        panel.addView(brand);
        TextView heading = new TextView(context);
        heading.setText(title);
        text(heading, 32, INK, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(context, 26);
        headingParams.bottomMargin = dp(context, 12);
        panel.addView(heading, headingParams);
        TextView detail = new TextView(context);
        detail.setText(introduction);
        text(detail, 15, MUTED, false);
        panel.addView(detail);
    }
}
