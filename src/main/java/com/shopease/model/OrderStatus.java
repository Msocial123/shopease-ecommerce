package com.shopease.model;

/** Order life-cycle. */
public enum OrderStatus {
    PENDING_PAYMENT("Awaiting payment"),
    PLACED("Placed (Cash on Delivery)"),
    PAID("Paid"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Customers may cancel until the parcel leaves the warehouse. */
    public boolean isCancellable() {
        return this == PENDING_PAYMENT || this == PLACED || this == PAID;
    }

    public boolean isAwaitingPayment() {
        return this == PENDING_PAYMENT;
    }

    public String getName() {
        return name();
    }
}
