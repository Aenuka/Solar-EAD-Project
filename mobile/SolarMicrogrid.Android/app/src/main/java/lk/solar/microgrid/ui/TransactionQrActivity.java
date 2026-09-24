package lk.solar.microgrid.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import lk.solar.microgrid.R;
import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.Reservation;
import lk.solar.microgrid.data.ReservationRepository;

public final class TransactionQrActivity extends Activity {
    private static final int GREEN = Color.rgb(23, 108, 77);
    private static final int INK = Color.rgb(23, 61, 50);
    private static final int MUTED = Color.rgb(107, 123, 117);

    private ReservationRepository reservations;
    private String reservationId;
    
    private LinearLayout content;
    private ProgressBar progress;
    private ImageView qrImage;
    private TextView detailsText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        reservations = ((SolarApplication) getApplication()).reservations();
        reservationId = getIntent().getStringExtra("reservationId");
        
        if (reservationId == null || reservationId.isEmpty()) {
            Toast.makeText(this, "Missing reservation identifier.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buildUi();
        loadTransaction();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 243));
        
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(32));
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        TextView brand = text(getString(R.string.brand), 12, GREEN, true);
        brand.setLetterSpacing(0.13f);
        
        TextView heading = text("Energy Transfer QR", 28, INK, true);
        ((LinearLayout.LayoutParams) heading.getLayoutParams()).topMargin = dp(24);
        
        TextView sub = text("Present this QR to the Grid Operator at the station.", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        ((LinearLayout.LayoutParams) sub.getLayoutParams()).bottomMargin = dp(24);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); 
        progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));

        qrImage = new ImageView(this);
        qrImage.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrlp = new LinearLayout.LayoutParams(dp(250), dp(250));
        qrlp.topMargin = dp(20);
        qrlp.bottomMargin = dp(20);
        content.addView(qrImage, qrlp);

        detailsText = text("", 15, INK, false);
        detailsText.setGravity(Gravity.CENTER_HORIZONTAL);
        detailsText.setVisibility(View.GONE);
    }

    private void loadTransaction() {
        progress.setVisibility(View.VISIBLE);
        reservations.getTransaction(reservationId, new ReservationRepository.Callback<Reservation>() {
            @Override
            public void success(Reservation result) {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                displayTransaction(result);
            }

            @Override
            public void failure(int status, String message) {
                if (isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(TransactionQrActivity.this, message, Toast.LENGTH_LONG).show();
                if (status == 401 || status == 403 || status == 404) {
                    finish();
                }
            }
        });
    }

    private void displayTransaction(Reservation r) {
        String token = r.source.optString("transactionToken", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "Failed to retrieve transaction token.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(token, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrImage.setImageBitmap(bitmap);
            qrImage.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR code.", Toast.LENGTH_LONG).show();
        }

        String details = "Reservation: " + (r.reservationId.isEmpty() ? r.id : r.reservationId) + "\n"
                       + "Station: " + r.stationId + "\n"
                       + "When: " + r.reservationDate + "\n"
                       + "Energy: " + r.energyAmountKwh + " kWh\n"
                       + "Type: " + r.tradingType + "\n"
                       + "Status: " + r.status;
        detailsText.setText(details);
        detailsText.setVisibility(View.VISIBLE);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        content.addView(v, lp);
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
