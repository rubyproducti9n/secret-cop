package com.rubyproducti9n.secretcop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getCurrentLocation(this, this, new BaseActivity.OnLocationResult() {
            @Override
            public void onLocationReceived(Location location) {
                checkCity(SplashActivity.this, SplashActivity.this, location);
            }

            @Override
            public void onLocationError(String errorMsg) {
                if (ActivityCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(SplashActivity.this, "Permission required", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            new MaterialAlertDialogBuilder(SplashActivity.this)
                                    .setTitle("Warning")
                                    .setMessage("To continue please restart the app.")
                                    .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finishAffinity();
                                        }
                                    }).setCancelable(false).show();
                        }
                    },2000);

                    return;
                }else{
                    new MaterialAlertDialogBuilder(SplashActivity.this)
                            .setTitle("Warning")
                            .setMessage("Unable to detect location. \n\nTry:\n1) Enabling Location in your device settings. \n2) Grant all the permission in app info. \n\nStill not working? Contact Us")
                            .setPositiveButton("Exit App", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(SplashActivity.this, SplashActivity.class));
                                    finish();
                                }
                            })
                            .setNegativeButton("Contact Us", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                                    emailIntent.setData(Uri.parse("mailto:")); // Only email apps should handle this
                                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"om.lokhande34@gmail.com"});
                                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Support Request – Secret Cop");
                                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Secret Cop Team,\n\nI would like to report an issue / provide feedback regarding the app.\n\nDetails:\n- [Explain your issue here]\n\nRegards,\n[Your Name]");

                                    try {
                                        startActivity(Intent.createChooser(emailIntent, "Send email via..."));
                                    } catch (android.content.ActivityNotFoundException ex) {
                                        Toast.makeText(SplashActivity.this, "No email client installed.", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            }).setCancelable(false).show();
                }
            }
        });
    }

    public static void getCurrentLocation(Context context, Activity activity, BaseActivity.OnLocationResult callback) {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            callback.onLocationError("Location permission not granted.");
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocationReceived(location);
            } else {
                callback.onLocationError("Unable to fetch location.");
            }
        }).addOnFailureListener(e -> {
            callback.onLocationError("Error fetching location: " + e.getMessage());
        });
    }

    public static void checkCity(Context context, Activity activity, Location location) {
        List<String> allowedCities = Arrays.asList("Mumbai", "Pune", "Hyderabad");
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String currentCity = addresses.get(0).getLocality();
                if (currentCity != null && allowedCities.contains(currentCity)) {
                    new Handler().postDelayed(() -> {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                        String uid = pref.getString("uid", null);
                        if (uid!=null){
                            context.startActivity(new Intent(context, MainActivity.class));
                            activity.finish();
                        }else{
                            context.startActivity(new Intent(context, LoginActivity.class));
                            activity.finish();
                        }
                    }, 2000);
                } else {
                    showExitDialog(activity, "Access Restricted", "This app is not available in your current location.");
                }
            } else {
                showExitDialog(activity, "Location Error", "Unable to determine your city.");
            }
        } catch (IOException e) {
            Log.e("City Location Error", "Trace: " + e.getMessage());
            showExitDialog(activity, "Location Error", "Failed to fetch location data.");
        }
    }

    private static void showExitDialog(Activity activity, String title, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Exit App", (dialog, which) -> {
                    activity.finishAffinity();
                    System.exit(0);
                }).show();
    }

    // Interface to handle async location result
    public interface OnLocationResult {
        void onLocationReceived(Location location);
        void onLocationError(String errorMsg);
    }
}