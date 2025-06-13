package com.rubyproducti9n.secretcop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {
    protected DatabaseReference dbRef;
    protected DatabaseReference usersRef;
    protected DatabaseReference couponsRef;
    protected FirebaseUser currentUser;
    public static SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbRef = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            usersRef = dbRef.child("users").child(currentUser.getUid());
            couponsRef = dbRef.child("coupons");
        }

         pref = PreferenceManager.getDefaultSharedPreferences(this);


    }

    public String getUid(){
        return pref.getString("uid", null);
    }
    public String getUserName(){
        return pref.getString("displayName", null);
    }

    public String getUserEmail(){
        return pref.getString("email", null);
    }

    public String getUserAvatar(){
        return pref.getString("photoUrl", null);
    }


    public static void getCurrentLocation(Context context, Activity activity, OnLocationResult callback) {
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
                        context.startActivity(new Intent(context, MainActivity.class));
                        activity.finish();
                    }, 2000);
                } else {
                    showExitDialog(activity, "Access Restricted", "This app is not available in your current location.");
                }
            } else {
                showExitDialog(activity, "Location Error", "Unable to determine your city.");
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    // 📌 Registers a new user with basic details in the "users" node
    protected void registerUser(String name, String email, String gender) {
        if (currentUser == null) return;

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("gender", gender);
        userMap.put("points", 0);
        userMap.put("rewards", new HashMap<>());

        usersRef.setValue(userMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) Log.d("BaseActivity", "User registered.");
        });
    }

    // 📌 Adds points to current user's account in a thread-safe way
    protected void addPointsToUser() {
        if (currentUser == null) return;

        usersRef.child("points").runTransaction(new Transaction.Handler() {
            @NonNull
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentPoints = currentData.getValue(Integer.class);
                currentData.setValue((currentPoints == null ? 0 : currentPoints) + 5);
                return Transaction.success(currentData);
            }

            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                Log.d("BaseActivity", "Points updated.");
            }
        });
    }

    // 📌 Fetches the full user object and triggers your custom listener
    protected void getUser(ValueEventListener listener) {
        if (currentUser != null) {
            usersRef.addListenerForSingleValueEvent(listener);
        }
    }

    // 📌 Claims a coupon: adds coupon UID under user rewards & marks coupon as claimed globally
    protected void claimCoupon(String couponUid) {
        if (currentUser == null) return;

        // Step 1: Add coupon to user's rewards
        usersRef.child("rewards").child(couponUid).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Step 2: Update coupon's isClaimed status
                        couponsRef.child(couponUid).child("isClaimed").setValue(true)
                                .addOnCompleteListener(inner -> {
                                    if (inner.isSuccessful()) {
                                        Log.d("BaseActivity", "Coupon claimed: " + couponUid);
                                    } else {
                                        Log.e("BaseActivity", "Coupon update failed", inner.getException());
                                    }
                                });
                    } else {
                        Log.e("BaseActivity", "Failed to update user's reward", task.getException());
                    }
                });
    }

    // 📌 Checks if user already has the given coupon in their rewards
    protected void isCouponClaimedByUser(String couponUid, ValueEventListener listener) {
        if (currentUser == null) return;

        usersRef.child("rewards").child(couponUid).addListenerForSingleValueEvent(listener);
    }

    // 📌 Fetches a coupon object by its UID
    protected void getCoupon(String couponUid, ValueEventListener listener) {
        couponsRef.child(couponUid).addListenerForSingleValueEvent(listener);
    }

    // 📌 Updates a specific field under the current user's node
    protected void updateUserField(String field, Object value) {
        if (currentUser == null) return;

        usersRef.child(field).setValue(value)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("BaseActivity", "Field updated: " + field);
                    }
                });
    }

    // 📌 Increments any numerical field under the user node
    protected void incrementUserField(String field, int by) {
        usersRef.child(field).runTransaction(new Transaction.Handler() {
            @NonNull
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer val = currentData.getValue(Integer.class);
                currentData.setValue((val == null ? 0 : val) + by);
                return Transaction.success(currentData);
            }

            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                Log.d("BaseActivity", "Incremented field: " + field);
            }
        });
    }

    // 📌 Deletes user data from DB + deletes their FirebaseAuth account
    protected void deleteUserAccount() {
        if (currentUser == null) return;

        usersRef.removeValue().addOnCompleteListener(task1 -> {
            currentUser.delete().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful()) {
                    Log.d("BaseActivity", "User deleted from auth & db");
                }
            });
        });
    }

    // 📌 Get current user's UID
    protected String getCurrentUid() {
        return (currentUser != null) ? currentUser.getUid() : null;
    }


}
