package lk.solar.microgrid.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import lk.solar.microgrid.R;

/** One set of destinations, shared by every primary screen. Detail flows keep their own back stack. */
public final class SolarNavigation {
    public static final String DESTINATION = "navigationDestination";
    public enum Tab {
        HOME(R.string.nav_home, R.drawable.ic_nav_home, MainActivity.class),
        EXPLORE(R.string.nav_explore, R.drawable.ic_nav_explore, StationMapActivity.class),
        BOOKINGS(R.string.nav_bookings, R.drawable.ic_nav_bookings, BookingHistoryActivity.class),
        ACCOUNT(R.string.nav_account, R.drawable.ic_nav_account, MainActivity.class),
        OVERVIEW(R.string.nav_overview, R.drawable.ic_nav_home, OperatorDashboardActivity.class),
        COMPLETED(R.string.nav_completed, R.drawable.ic_nav_bookings, CompletedOperationsActivity.class),
        OPERATOR_ACCOUNT(R.string.nav_account, R.drawable.ic_nav_account, OperatorProfileActivity.class);

        final int label, icon;
        final Class<? extends Activity> activity;
        Tab(int label, int icon, Class<? extends Activity> activity) {
            this.label = label; this.icon = icon; this.activity = activity;
        }
        boolean operator() { return this == OVERVIEW || this == COMPLETED || this == OPERATOR_ACCOUNT; }
    }
    private SolarNavigation() { }

    static View bar(SolarActivity activity, Tab selected) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.WHITE);
        View divider = new View(activity);
        divider.setBackgroundColor(SolarStyle.BORDER);
        container.addView(divider, new LinearLayout.LayoutParams(-1, 1));
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(SolarStyle.dp(activity, 8), SolarStyle.dp(activity, 4), SolarStyle.dp(activity, 8), SolarStyle.dp(activity, 4));
        container.addView(row, new LinearLayout.LayoutParams(-1, -2));
        Tab[] tabs = selected.operator() ? new Tab[]{Tab.OVERVIEW, Tab.COMPLETED, Tab.OPERATOR_ACCOUNT}
                : new Tab[]{Tab.HOME, Tab.EXPLORE, Tab.BOOKINGS, Tab.ACCOUNT};
        for (Tab tab : tabs) {
            boolean active = tab == selected;
            TextView item = new TextView(activity);
            item.setText(tab.label);
            SolarStyle.text(item, 12, active ? SolarStyle.BLUE : SolarStyle.MUTED, active);
            // Tab captions use compact type; the target itself stays at least 56 dp tall.
            item.setTextSize(12);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, SolarStyle.dp(activity, 8), 0, SolarStyle.dp(activity, 8));
            Drawable icon = activity.getDrawable(tab.icon);
            if (icon != null) {
                icon = icon.mutate();
                icon.setTint(active ? SolarStyle.BLUE : SolarStyle.MUTED);
                icon.setBounds(0, 0, SolarStyle.dp(activity, 24), SolarStyle.dp(activity, 24));
            }
            item.setCompoundDrawables(null, icon, null, null);
            item.setCompoundDrawablePadding(SolarStyle.dp(activity, 4));
            item.setBackground(new RippleDrawable(ColorStateList.valueOf(SolarStyle.RIPPLE),
                    SolarStyle.shape(activity, active ? SolarStyle.SKY : Color.TRANSPARENT, 16, 0), null));
            item.setSelected(active);
            item.setContentDescription(activity.getString(tab.label) + (active ? activity.getString(R.string.nav_selected) : ""));
            item.setFocusable(true);
            item.setMinimumHeight(SolarStyle.dp(activity, 56));
            item.setOnClickListener(v -> { if (!active) activity.selectTab(tab); });
            row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return container;
    }

    public static void open(SolarActivity source, Tab tab) {
        Class<? extends Activity> root = tab.operator() ? OperatorDashboardActivity.class : MainActivity.class;
        // Rebase tab changes on the role's root, including destinations opened from a detail flow.
        Class<? extends Activity> destination = source.getClass() == root ? tab.activity : root;
        Intent intent = new Intent(source, destination)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(DESTINATION, tab.name());
        source.startActivity(intent);
        // The launch screen stays as the root. Switching sibling tabs never piles up activities.
        if (!(source instanceof MainActivity) && !(source instanceof OperatorDashboardActivity)) source.finish();
    }
}
