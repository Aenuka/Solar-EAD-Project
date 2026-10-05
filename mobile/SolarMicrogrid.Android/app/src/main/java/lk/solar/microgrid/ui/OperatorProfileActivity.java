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

public class OperatorProfileActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN, INK = SolarStyle.INK, MUTED = SolarStyle.MUTED;
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
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(36));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));


        setContentView(scroll);
        scroll.requestApplyInsets();

        // Top Navigation / Back button
        TextView backBtn = new TextView(this);
        backBtn.setText("← Back");
        backBtn.setTextSize(14); backBtn.setTextColor(GREEN); backBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        backBtn.setPadding(0, dp(10), 0, dp(20));
        backBtn.setClickable(true);
        backBtn.setOnClickListener(v -> finish());
        content.addView(backBtn);

        // Profile Header Section (Avatar + Name + Role Badge)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.topMargin = dp(20);
        headerParams.bottomMargin = dp(32);
        content.addView(header, headerParams);

        android.widget.ImageView avatar = new android.widget.ImageView(this);
        avatar.setImageResource(R.drawable.ic_account_avatar);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(100), dp(100));
        avatarParams.bottomMargin = dp(16);
        header.addView(avatar, avatarParams);

        TextView name = new TextView(this);
        name.setText(operators.getFullName() != null ? operators.getFullName() : "Operator");
        name.setTextSize(24); name.setTextColor(INK); name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        header.addView(name);

        TextView roleBadge = new TextView(this);
        roleBadge.setText("GRID OPERATOR");
        roleBadge.setTextSize(11);
        roleBadge.setTextColor(GREEN);
        roleBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        roleBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        android.graphics.drawable.GradientDrawable badgeBg = shape(Color.rgb(234, 247, 239), 0);
        badgeBg.setCornerRadius(dp(16));
        roleBadge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        badgeParams.topMargin = dp(12);
        header.addView(roleBadge, badgeParams);

        // Account Details Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        SolarStyle.card(card);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));

        if (operators.getUsername() != null && !operators.getUsername().isEmpty()) {
            detail(card, "Username", operators.getUsername());
        }
        detail(card, "Role", "Grid Operator");

        // Sign Out Button
        Button signoutButton = new Button(this);
        signoutButton.setText("Sign Out");
        signoutButton.setTextSize(14); signoutButton.setAllCaps(false);
        signoutButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        signoutButton.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable btnBg = shape(SolarStyle.RED, 0);
        btnBg.setCornerRadius(dp(25));
        signoutButton.setBackground(btnBg);
        signoutButton.setElevation(0);
        signoutButton.setMinHeight(dp(52));
        LinearLayout.LayoutParams btnLayout = new LinearLayout.LayoutParams(-1, -2);
        btnLayout.topMargin = dp(40);
        content.addView(signoutButton, btnLayout);

        signoutButton.setOnClickListener(view -> {
            new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out of your session?")
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
    }

    private void detail(LinearLayout parent, String label, String value) {
        TextView viewLabel = new TextView(this);
        viewLabel.setText(label);
        viewLabel.setTextSize(13);
        viewLabel.setTextColor(MUTED);
        parent.addView(viewLabel);

        TextView viewValue = new TextView(this);
        viewValue.setText(value);
        viewValue.setTextSize(16);
        viewValue.setTextColor(INK);
        viewValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.bottomMargin = dp(8);
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
