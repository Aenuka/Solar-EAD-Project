package lk.solar.microgrid.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.OperatorRepository;

public class OperatorDashboardActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);
    private OperatorRepository operators;
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        operators = ((SolarApplication) getApplication()).operators();
        if (!operators.signedIn()) {
            startActivity(new Intent(this, OperatorLoginActivity.class));
            finish();
            return;
        }
        showDashboard();
    }

    private void showDashboard() {
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
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        
        setContentView(scroll);
        scroll.requestApplyInsets();
        
        TextView heading = text(getString(R.string.operator_dashboard_title), 32, INK, true);
        LinearLayout.LayoutParams headingLayout = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(12);
        
        text(operators.getFullName() != null ? operators.getFullName() : "Operator", 24, INK, true);
        
        button(R.string.scan_transaction_qr, true, () -> {
            startActivity(new Intent(this, QrScannerActivity.class));
        });
        
        button(R.string.sign_out, false, () -> {
            operators.logout();
            startActivity(new Intent(this, OperatorLoginActivity.class));
            finish();
        });
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

    private Button button(int label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(13); button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : GREEN);
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : Color.rgb(234, 240, 227)));
        button.setMinHeight(dp(50));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.topMargin = dp(15);
        content.addView(button, layout);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
