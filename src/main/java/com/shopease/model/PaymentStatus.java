package com.shopease.model;

public enum PaymentStatus {
    SUCCESS, FAILED, PENDING, REFUNDED;

    public String getName() {
        return name();
    }
}
