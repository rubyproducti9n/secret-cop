package com.rubyproducti9n.secretcop;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends BaseActivity {
    private TextView tvPoints;
    private TextView tvRewards;

    private DatabaseReference userRef;

    private int points = 0;
    private int rewards = 0;

    //Show a dialog box if service is not available in mentioned city yes waw

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase Init
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        // UI Bindings
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvPoints = findViewById(R.id.tvPoints);
        tvRewards = findViewById(R.id.tvRewards);
        MaterialButton btnViewRewards = findViewById(R.id.btnViewRewards);
        FloatingActionButton fabReportNow = findViewById(R.id.fabReportNow);

        String name = currentUser.getDisplayName();
        tvGreeting.setText("Hi, " + (name != null ? name : "Hero") + "! 👋");

        // Load user stats
        loadUserStats();

        // Report Now Click
        fabReportNow.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });
        fabReportNow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                String details = getPreference("displayName", null)
                        + (String) getPreference("first_name", null)
                        + getPreference("last_name", null)
                        + getPreference("email", null)
                        + getPreference("uid", null)
                        + getPreference("gender", null)
                        + getPreference("contact", null);
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Details")
                        .setMessage(details)
                        .show();
                return false;
            }
        });

        // Rewards Click
        btnViewRewards.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RewardsActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserStats() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer pointsValue = snapshot.child("points").getValue(Integer.class);
                    points = (pointsValue != null) ? pointsValue : 0;

                    DataSnapshot rewardsSnapshot = snapshot.child("rewards");
                    rewards = (rewardsSnapshot.exists()) ? (int) rewardsSnapshot.getChildrenCount() : 0;

                    tvPoints.setText("Your Points: " + points);
                    tvRewards.setText("Rewards Unlocked: 🎁 " + rewards);
                } else {
                    userRef.child("points").setValue(0);
                    userRef.child("rewards").removeValue(); // Remove entire branch if initializing
                    tvPoints.setText("Your Points: 0");
                    tvRewards.setText("Rewards Unlocked: 🎁 0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    @SuppressLint("SetTextI18n")
//    public void addPoints(int value) {
//        points += value;
//        userRef.child("points").setValue(points);
//        tvPoints.setText("Your Points: " + points);
//
//        // Check reward eligibility
//        if (points >= 100) {
//            rewards++;
//            points -= 100; // reset 100 pts
//            userRef.child("rewards").setValue(rewards);
//            userRef.child("points").setValue(points);
//            tvRewards.setText("Rewards Unlocked: 🎁 " + rewards);
//            tvPoints.setText("Your Points: " + points);
//            Toast.makeText(this, "You unlocked a reward!", Toast.LENGTH_LONG).show();
//            // Trigger GPay Coupon Logic here if applicable
//        }
//    }
}