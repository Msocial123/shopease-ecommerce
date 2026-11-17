package com.shopease.model;

public enum PaymentMethod {
    CARD("Credit / Debit Card"),
    UPI("UPI"),
    NETBANKING("Net Banking"),
    COD("Cash on Delivery");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name();
    }
}
