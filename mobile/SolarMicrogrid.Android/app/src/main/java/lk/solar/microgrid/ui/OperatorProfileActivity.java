package lk.solar.microgrid.ui;

import android.app.Activity;
import android.app.AlertDialog;
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

public class OperatorProfileActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77), INK = Color.rgb(23, 61, 50), MUTED = Color.rgb(107, 123, 117);
    private OperatorRepository operators;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operators = ((SolarApplication) getApplication()).operators();
        if (!operators.signedIn()) {
            finish();
            return;
        }

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
        
        TextView heading = new TextView(this);
        heading.setText("Operator Profile");
        heading.setTextSize(32); heading.setTextColor(INK); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams headingLayout = new LinearLayout.LayoutParams(-1, -2);
        headingLayout.topMargin = dp(30); headingLayout.bottomMargin = dp(20);
        content.addView(heading, headingLayout);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(Color.WHITE, 0));
        card.setElevation(dp(4));
        card.setPadding(dp(24), dp(32), dp(24), dp(24));
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));

        android.widget.ImageView avatar = new android.widget.ImageView(this);
        avatar.setImageResource(R.drawable.ic_account_avatar);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        avatarParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = dp(24);
        card.addView(avatar, avatarParams);

        detail(card, "Full Name", operators.getFullName());
        if (operators.getUsername() != null && !operators.getUsername().isEmpty()) {
            detail(card, "Username", operators.getUsername());
        }
        detail(card, "Role", "Grid Operator");

        Button signoutButton = new Button(this);
        signoutButton.setText("Sign Out");
        signoutButton.setTextSize(14); signoutButton.setAllCaps(false);
        signoutButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        signoutButton.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable btnBg = shape(Color.rgb(211, 47, 47), 0);
        btnBg.setCornerRadius(dp(25));
        signoutButton.setBackground(btnBg);
        signoutButton.setElevation(dp(4));
        signoutButton.setMinHeight(dp(50));
        LinearLayout.LayoutParams btnLayout = new LinearLayout.LayoutParams(-1, -2);
        btnLayout.topMargin = dp(40);
        content.addView(signoutButton, btnLayout);
        
        signoutButton.setOnClickListener(view -> {
            new AlertDialog.Builder(this)
                .setTitle("Sign Out?")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    operators.logout();
                    Intent intent = new Intent(this, OperatorLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        Button backButton = new Button(this);
        backButton.setText("Back");
        backButton.setTextSize(14); backButton.setAllCaps(false);
        backButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        backButton.setTextColor(MUTED);
        backButton.setBackground(null);
        backButton.setElevation(0);
        backButton.setMinHeight(dp(50));
        LinearLayout.LayoutParams layout2 = new LinearLayout.LayoutParams(-1, -2);
        layout2.topMargin = dp(10);
        content.addView(backButton, layout2);
        backButton.setOnClickListener(view -> finish());
    }

    private void detail(LinearLayout parent, String label, String value) {
        TextView viewLabel = new TextView(this);
        viewLabel.setText(label);
        viewLabel.setTextSize(12);
        viewLabel.setTextColor(MUTED);
        parent.addView(viewLabel);
        
        TextView viewValue = new TextView(this);
        viewValue.setText(value);
        viewValue.setTextSize(16);
        viewValue.setTextColor(INK);
        viewValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(16);
        parent.addView(viewValue, layout);
    }

    private android.graphics.drawable.GradientDrawable shape(int fill, int stroke) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable(); 
        shape.setColor(fill); shape.setCornerRadius(dp(8));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
