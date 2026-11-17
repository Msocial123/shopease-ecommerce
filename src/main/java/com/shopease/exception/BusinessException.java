package com.shopease.exception;

/** A rule of the shop was violated (out of stock, invalid card, ...). Message is safe to show to users. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
