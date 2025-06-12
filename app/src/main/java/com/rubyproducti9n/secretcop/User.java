package com.rubyproducti9n.secretcop;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    public String uid;
    public String username; // Corresponds to displayName
    public String email;
    public String phoneNumber; // Can be null
    public String profileImageUrl; // Can be null
    public Long createdAt; // Timestamp in milliseconds
    public Long lastLoginAt; // Timestamp in milliseconds
    public String providerId; // e.g., "google.com", "password"

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String uid, String username, String email, String phoneNumber, String profileImageUrl, Long createdAt, Long lastLoginAt, String providerId) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.providerId = providerId;
    }

    // Public getters are required by Firebase Realtime Database for reading data back
    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getLastLoginAt() {
        return lastLoginAt;
    }

    public String getProviderId() {
        return providerId;
    }
}
