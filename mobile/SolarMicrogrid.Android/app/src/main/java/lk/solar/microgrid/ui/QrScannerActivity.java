package lk.solar.microgrid.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.google.zxing.ResultPoint;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.Typeface;

import lk.solar.microgrid.SolarApplication;
import lk.solar.microgrid.data.OperatorRepository;
import lk.solar.microgrid.data.Reservation;

import java.util.List;

public class QrScannerActivity extends Activity {
    private DecoratedBarcodeView barcodeView;
    private boolean isVerifying = false;
    private OperatorRepository operators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operators = ((SolarApplication) getApplication()).operators();
        
        barcodeView = new DecoratedBarcodeView(this);
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.rgb(245, 247, 243));
        
        TextView title = new TextView(this);
        title.setText("Scan Transaction QR");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(23, 61, 50));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(dp(26), dp(30), dp(26), dp(8));
        content.addView(title);
        
        TextView instructions = new TextView(this);
        instructions.setText("Align the Prosumer QR inside the frame");
        instructions.setTextSize(14);
        instructions.setTextColor(Color.rgb(107, 123, 117));
        instructions.setPadding(dp(26), 0, dp(26), dp(20));
        content.addView(instructions);

        LinearLayout.LayoutParams scannerParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        content.addView(barcodeView, scannerParams);

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setTextColor(Color.rgb(23, 108, 77));
        cancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(234, 240, 227)));
        cancel.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-1, dp(50));
        btnParams.setMargins(dp(26), dp(20), dp(26), dp(20));
        content.addView(cancel, btnParams);

        setContentView(content);
        
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startScanning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (isVerifying || result.getText() == null) return;
                isVerifying = true;
                barcodeView.pause();
                
                Toast.makeText(QrScannerActivity.this, "Verifying transaction...", Toast.LENGTH_SHORT).show();
                
                String token = result.getText();
                verifyToken(token);
            }
            @Override
            public void possibleResultPoints(List<ResultPoint> points) {}
        });
    }

    private void verifyToken(String token) {
        operators.verifyTransaction(token, new OperatorRepository.Callback<Reservation>() {
            @Override
            public void success(Reservation reservation) {
                if (isDestroyed() || isFinishing()) return;
                Intent intent = new Intent(QrScannerActivity.this, TransactionVerificationActivity.class);
                intent.putExtra("reservationJson", reservation.source.toString());
                startActivity(intent);
                finish();
            }

            @Override
            public void failure(int status, String message) {
                if (isDestroyed() || isFinishing()) return;
                
                String displayMessage = message;
                if (status == 404 || status == 400 && message.contains("Invalid")) {
                    displayMessage = "Invalid transaction QR.";
                } else if (status == 0) {
                    displayMessage = "Unable to contact the server.";
                }

                new AlertDialog.Builder(QrScannerActivity.this)
                    .setTitle("Error")
                    .setMessage(displayMessage)
                    .setPositiveButton("Scan Again", (dialog, which) -> {
                        isVerifying = false;
                        barcodeView.resume();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isVerifying && barcodeView != null) barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) barcodeView.pause();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
