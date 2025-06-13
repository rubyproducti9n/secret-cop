package com.rubyproducti9n.secretcop;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    public String uid;
    public String firstName;

    public String lastName;
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

    public User(String uid, String firstName, String lastName, String username, String email, String phoneNumber, String profileImageUrl, Long createdAt, Long lastLoginAt, String providerId) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
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

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastLoginAt(Long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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
