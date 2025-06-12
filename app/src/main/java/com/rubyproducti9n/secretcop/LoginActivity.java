package com.rubyproducti9n.secretcop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "GoogleSignInActivity";
    private static final String PREFS_NAME = "UserSessionPrefs"; // Name for SharedPreferences file

    // UI elements
    private TextView statusTextView;
    private SignInButton googleSignInButton;
    private Button signOutButton;
    private CircularProgressIndicator progressBar;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // Google Sign-In Client
    private GoogleSignInClient mGoogleSignInClient;

    // ActivityResultLauncher for Google Sign-In
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Firebase Realtime Database
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize UI elements
        statusTextView = findViewById(R.id.statusTextView);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        signOutButton = findViewById(R.id.signOutButton);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Realtime Database reference
        // Use getReference() to get a reference to the root of your database.
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Configure Google Sign In to request the user's ID, email address, and basic profile.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Crucial for Firebase Auth
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up the ActivityResultLauncher for handling the Google Sign-In flow result
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleSignInResult(task);
                    } else {
                        Log.w(TAG, "Google Sign-In cancelled or failed with code: " + result.getResultCode());
                        Toast.makeText(LoginActivity.this, "Google Sign-In cancelled.", Toast.LENGTH_SHORT).show();
                        hideProgressBar();
                    }
                });

        // Set click listeners
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        signOutButton.setOnClickListener(v -> signOut());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is already signed in, skip to HomeActivity
            Log.d(TAG, "User already logged in: " + currentUser.getEmail() + ". Redirecting to HomeActivity.");
            redirectToHomeActivity();
        } else {
            // No user is signed in, display the sign-in UI
            updateUI(null); // This ensures the sign-in button is visible
        }
    }

    private void signInWithGoogle() {
        showProgressBar();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Google Sign In was successful, authenticate with Firebase
            Log.d(TAG, "Google sign in successful: " + account.getId());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            // Google Sign In failed, update UI accordingly
            Log.w(TAG, "Google sign in failed", e);
            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            updateUI(null); // Show signed out UI
            hideProgressBar();
        }
    }

    /**
     * Displays a Material AlertDialog to confirm user details before saving and proceeding.
//     * @param user The FirebaseUser object whose details are to be displayed.
     */
//    private void showUserDetailsConfirmation(FirebaseUser user) {
//        // Inflate the custom layout for the dialog
//        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_user_details_dialog, null);
//
//        // Get UI elements from the custom layout
//        ImageView profileImage = dialogView.findViewById(R.id.dialogProfileImage);
//        TextView usernameTv = dialogView.findViewById(R.id.dialogUsername);
//        TextView emailTv = dialogView.findViewById(R.id.dialogEmail);
//        TextView uidTv = dialogView.findViewById(R.id.dialogUid);
//        TextView phoneTv = dialogView.findViewById(R.id.dialogPhone);
//        TextView providerTv = dialogView.findViewById(R.id.dialogProvider);
//        TextView createdAtTv = dialogView.findViewById(R.id.dialogCreatedAt);
//        TextView lastLoginTv = dialogView.findViewById(R.id.dialogLastLogin);
//
//        // Populate details
//        usernameTv.setText("Username: " + (user.getDisplayName() != null ? user.getDisplayName() : "N/A"));
//        emailTv.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"));
//        uidTv.setText("UID: " + (user.getUid() != null ? user.getUid() : "N/A"));
//
//        // Load profile image using Glide
//        if (user.getPhotoUrl() != null) {
//            Picasso.get().load(user.getPhotoUrl()).into(profileImage);
//        } else {
//            profileImage.setImageResource(android.R.drawable.sym_def_app_icon); // Default icon if no photo
//        }
//
//        // Get phone number from provider data (Google sign-in itself doesn't provide phone directly)
//        String phoneNumber = "N/A";
//        String providerId = "N/A";
//        if (user.getProviderData() != null) {
//            for (UserInfo profile : user.getProviderData()) {
//                if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()) {
//                    phoneNumber = profile.getPhoneNumber();
//                }
//                if (profile.getProviderId() != null && !profile.getProviderId().isEmpty()) {
//                    // This often gives the primary provider that authenticated the user
//                    // (e.g., "google.com", "password", "phone")
//                    providerId = profile.getProviderId();
//                }
//            }
//        }
//        phoneTv.setText("Phone: " + phoneNumber);
//        providerTv.setText("Provider: " + providerId);
//
//
//        // Format and display timestamps
//        if (user.getMetadata() != null) {
//            long creationTimestamp = user.getMetadata().getCreationTimestamp();
//            long lastSignInTimestamp = user.getMetadata().getLastSignInTimestamp();
//
//            createdAtTv.setText("Created At: " + DateFormat.format("MMM d, yyyy HH:mm", new Date(creationTimestamp)));
//            lastLoginTv.setText("Last Login: " + DateFormat.format("MMM d, yyyy HH:mm", new Date(lastSignInTimestamp)));
//        } else {
//            createdAtTv.setText("Created At: N/A");
//            lastLoginTv.setText("Last Login: N/A");
//        }
//
//
//        // Build and show the Material Alert Dialog
//        new MaterialAlertDialogBuilder(this)
//                .setView(dialogView) // Set the custom layout as the dialog view
//                .setCancelable(false) // User must confirm or cancel
//                .setPositiveButton("Continue", (dialog, which) -> {
//                    // User confirmed, save details and proceed
//                    saveUserDetailsToSharedPreferences(user);
//                    saveUserDetailsToDatabase(user);
//                    redirectToHomeActivity();
//                    dialog.dismiss(); // Dismiss the dialog
//                })
//                .setNegativeButton("Cancel", (dialog, which) -> {
//                    // User cancelled, sign out
//                    Toast.makeText(LoginActivity.this, "Sign-in cancelled by user.", Toast.LENGTH_SHORT).show();
//                    signOut(); // Perform full sign out
//                    dialog.dismiss(); // Dismiss the dialog
//                })
//                .show();
//    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideProgressBar();
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "Firebase Google Auth successful");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {

                            saveUserDetailsToSharedPreferences(user);
                            saveUserDetailsToDatabase(user);
                            redirectToHomeActivity();
//                            showUserDetailsConfirmation(user); // Show confirmation dialog
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "Firebase Google Auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void signOut() {
        showProgressBar();
        mAuth.signOut(); // Sign out from Firebase

        // Clear user details from SharedPreferences
        clearUserDetailsFromSharedPreferences();

        // Sign out from Google (optional, but good for consistency)
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            hideProgressBar();
            if (task.isSuccessful()) {
                Log.d(TAG, "Google Sign-Out successful");
                updateUI(null); // Update UI to signed-out state
                Toast.makeText(LoginActivity.this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Google Sign-Out failed", task.getException());
                Toast.makeText(LoginActivity.this, "Google Sign-Out failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Saves available FirebaseUser details to SharedPreferences.
     * This data is typically for quick local access (e.g., displaying username instantly).
     *
     * @param user The FirebaseUser object.
     */
    private void saveUserDetailsToSharedPreferences(FirebaseUser user) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
//        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();

        editor.putString("uid", user.getUid());
        editor.putString("email", user.getEmail());
        editor.putString("displayName", user.getDisplayName());
        editor.putString("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        editor.putBoolean("isEmailVerified", user.isEmailVerified());

        // Get phone number from provider data (Google sign-in itself doesn't provide phone directly)
        String phoneNumber = null;
        if (user.getProviderData() != null) {
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()) {
                    phoneNumber = profile.getPhoneNumber();
                    break; // Found a phone number, take the first one
                }
            }
        }
        editor.putString("phoneNumber", phoneNumber);

        // Account creation and last sign-in timestamps
        if (user.getMetadata() != null) {
            editor.putLong("creationTimestamp", user.getMetadata().getCreationTimestamp());
            editor.putLong("lastSignInTimestamp", user.getMetadata().getLastSignInTimestamp());
        }

        // Apply changes asynchronously
        editor.apply();
        Log.d(TAG, "User details saved to SharedPreferences for UID: " + user.getUid());
    }

    /**
     * Clears all user details stored in SharedPreferences for this session.
     */
    private void clearUserDetailsFromSharedPreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.clear(); // Clears all key-value pairs in this SharedPreferences file
        editor.apply();
        Log.d(TAG, "User details cleared from SharedPreferences.");
    }

    /**
     * Pushes user details to Firebase Realtime Database.
     * This function creates or updates a node under "users/{uid}" with user data.
     *
     * @param user The FirebaseUser object.
     */
    private void saveUserDetailsToDatabase(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "Attempted to save null FirebaseUser to database.");
            return;
        }

        String uid = user.getUid();
        String username = user.getDisplayName();
        String email = user.getEmail();
        Uri photoUrl = user.getPhotoUrl();
        Long createdAt = user.getMetadata() != null ? user.getMetadata().getCreationTimestamp() : null;
        Long lastLoginAt = user.getMetadata() != null ? user.getMetadata().getLastSignInTimestamp() : null;

        // Determine the primary sign-in provider (e.g., "google.com", "password", "phone")
        String providerId = null;
        if (user.getProviderData() != null && !user.getProviderData().isEmpty()) {
            providerId = user.getProviderData().get(0).getProviderId(); // Get the primary provider
        }

        // Try to get phone number from provider data (Google sign-in doesn't provide phone directly)
        String phoneNumber = null;
        if (user.getProviderData() != null) {
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()) {
                    phoneNumber = profile.getPhoneNumber();
                    break; // Found a phone number, take the first one
                }
            }
        }

        // Create a User object from your model class
        User newUser = new User(
                uid,
                username,
                email,
                phoneNumber,
                photoUrl != null ? photoUrl.toString() : null,
                createdAt,
                lastLoginAt,
                providerId
        );

        // Push the user object to the "users" node in Realtime Database under their UID
        // Using setValue() will overwrite any existing data at this path.
        mDatabase.child("users").child(uid).setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data successfully written to Realtime Database for UID: " + uid);
                    Toast.makeText(LoginActivity.this, "User data saved to database.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing user data to Realtime Database for UID: " + uid, e);
                    Toast.makeText(LoginActivity.this, "Failed to save user data to database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //region UI Management
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // This path should ideally not be hit as we redirect to HomeActivity
            // but kept for fallback/debug purposes for MainActivity's own UI
//            String userInfo = "Signed In as:\n" + (user.getDisplayName() != null ? user.getDisplayName() : "N/A Name") + "\n(" + (user.getEmail() != null ? user.getEmail() : "N/A Email") + ")";
//            statusTextView.setText(userInfo);
            googleSignInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE); // signOutButton will still be visible in MainActivity if user remains here
            statusTextView.setVisibility(View.VISIBLE);
        } else {
            //statusTextView.setText("Signed Out");
            googleSignInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
            statusTextView.setVisibility(View.VISIBLE);
        }
        hideProgressBar(); // Ensure progress bar is hidden after UI update
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        googleSignInButton.setEnabled(false); // Disable button while loading
        signOutButton.setEnabled(false);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        googleSignInButton.setEnabled(true); // Re-enable button
        signOutButton.setEnabled(true);
    }
    /**
     * Redirects the user to the HomeActivity and finishes the current MainActivity.
     */
    private void redirectToHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Clear the back stack so pressing back from HomeActivity doesn't return to MainActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish MainActivity so user can't navigate back to it
    }
    //endregion
}