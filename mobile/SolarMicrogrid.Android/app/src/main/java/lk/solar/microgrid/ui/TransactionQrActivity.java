package lk.solar.microgrid.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

public final class TransactionQrActivity extends SolarActivity {
    private static final int GREEN = SolarStyle.GREEN;
    private static final int INK = SolarStyle.INK;
    private static final int MUTED = SolarStyle.MUTED;

    private ReservationRepository reservations;
    private LinearLayout content;
    private ProgressBar progress;
    private ImageView qrCodeView;
    private TextView errorText;
    private Button closeBtn;

    private String id;
    private String reservationCode;
    private String stationId;
    private String reservationDate;
    private double energyAmountKwh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reservations = ((SolarApplication) getApplication()).reservations();

        id = getIntent().getStringExtra("id");
        reservationCode = getIntent().getStringExtra("reservationCode");
        stationId = getIntent().getStringExtra("stationId");
        reservationDate = getIntent().getStringExtra("reservationDate");
        energyAmountKwh = getIntent().getDoubleExtra("energyAmountKwh", 0);

        buildUi();
        fetchToken();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(SolarStyle.BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("Transaction QR");
        title.setTextSize(28);
        title.setTextColor(INK);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Present this QR to the Grid Operator.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(MUTED);
        subtitle.setPadding(0, dp(8), 0, dp(24));
        content.addView(subtitle);

        // Booking details
        LinearLayout detailsCard = new LinearLayout(this);
        detailsCard.setOrientation(LinearLayout.VERTICAL);
        SolarStyle.card(detailsCard);
        detailsCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.addView(detailsCard, new LinearLayout.LayoutParams(-1, -2));

        addRow(detailsCard, "Booking ID", reservationCode);
        addRow(detailsCard, "Date/Time", reservationDate);
        addRow(detailsCard, "Energy", energyAmountKwh + " kWh");
        addRow(detailsCard, "Status", "APPROVED");

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(48), dp(48));
        plp.topMargin = dp(32);
        content.addView(progress, plp);

        qrCodeView = new ImageView(this);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(dp(250), dp(250));
        qlp.topMargin = dp(24);
        qrCodeView.setLayoutParams(qlp);
        qrCodeView.setVisibility(View.GONE);
        content.addView(qrCodeView);

        errorText = new TextView(this);
        errorText.setTextColor(SolarStyle.RED);
        errorText.setTextSize(14);
        errorText.setPadding(0, dp(16), 0, 0);
        errorText.setVisibility(View.GONE);
        content.addView(errorText);

        closeBtn = new Button(this);
        closeBtn.setText(R.string.close_button);
        closeBtn.setAllCaps(false);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackgroundColor(GREEN);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(48));
        blp.topMargin = dp(32);
        content.addView(closeBtn, blp);
        closeBtn.setOnClickListener(v -> finish());
    }

    private void addRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));

        TextView l = new TextView(this);
        l.setText(label + ": ");
        l.setTextColor(MUTED);
        l.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(INK);
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private void fetchToken() {
        if (id == null || id.isEmpty()) {
            showError("Invalid reservation ID.");
            return;
        }

        reservations.transaction(id, new ReservationRepository.Callback<Reservation>() {
            @Override
            public void success(Reservation result) {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);

                String token = result.transactionToken;
                if (token == null || token.isEmpty()) {
                    showError("No transaction token received from the server.");
                } else {
                    displayQrCode(token);
                }
            }

            @Override
            public void failure(int status, String message) {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);

                if (status == 400 || status == 403 || status == 404 || status == 401) {
                    showError(message);
                } else {
                    showError("Unable to contact the server.");
                }
            }
        });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void displayQrCode(String token) {
        try {
            BitMatrix result = new MultiFormatWriter().encode(token, BarcodeFormat.QR_CODE, 512, 512, null);
            int w = result.getWidth();
            int h = result.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, 512, 0, 0, w, h);

            qrCodeView.setImageBitmap(bitmap);
            qrCodeView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            showError("Failed to generate QR code.");
        }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
