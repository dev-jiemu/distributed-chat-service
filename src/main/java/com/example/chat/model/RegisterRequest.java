package com.example.chat.model;

import java.util.Objects;

public class RegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String currentUserId;

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String confirmPassword, String currentUserId) {
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.currentUserId = currentUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRequest that = (RegisterRequest) o;
        return Objects.equals(email, that.email) && Objects.equals(password, that.password) && Objects.equals(confirmPassword, that.confirmPassword) && Objects.equals(currentUserId, that.currentUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password, confirmPassword, currentUserId);
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", confirmPassword='" + confirmPassword + '\'' +
                ", currentUserId='" + currentUserId + '\'' +
                '}';
    }
}
