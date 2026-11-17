package com.shopease.service;

import com.shopease.model.PaymentMethod;

/** Payment form data. Card number and CVV are only held in memory, never persisted. */
public class PaymentRequest {
    private PaymentMethod method;
    private String cardName;
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String upiId;
    private String bank;

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }
    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
}
