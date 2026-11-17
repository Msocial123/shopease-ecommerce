package com.shopease.service;

import com.shopease.exception.BusinessException;
import com.shopease.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService(null, null);

    private static PaymentRequest card(String number, String expiry, String cvv) {
        PaymentRequest r = new PaymentRequest();
        r.setMethod(PaymentMethod.CARD);
        r.setCardName("Test User");
        r.setCardNumber(number);
        r.setCardExpiry(expiry);
        r.setCardCvv(cvv);
        return r;
    }

    private static String futureExpiry() {
        YearMonth ym = YearMonth.now().plusYears(2);
        return String.format("%02d/%02d", ym.getMonthValue(), ym.getYear() % 100);
    }

    @Test
    void luhn() {
        assertTrue(PaymentService.luhnValid("4111111111111111"));
        assertTrue(PaymentService.luhnValid("5555555555554444"));
        assertFalse(PaymentService.luhnValid("4111111111111112"));
    }

    @Test
    void brandDetection() {
        assertEquals("VISA", PaymentService.cardBrand("4111111111111111"));
        assertEquals("MASTERCARD", PaymentService.cardBrand("5555555555554444"));
        assertEquals("AMEX", PaymentService.cardBrand("378282246310005"));
        assertEquals("RUPAY", PaymentService.cardBrand("6071234567890123"));
    }

    @Test
    void validCardPassesValidation() {
        assertEquals("4111111111111111", service.validateCard(card("4111 1111 1111 1111", futureExpiry(), "123")));
    }

    @Test
    void rejectsBadCards() {
        assertThrows(BusinessException.class, () -> service.validateCard(card("4111 1111 1111 1112", futureExpiry(), "123")));
        assertThrows(BusinessException.class, () -> service.validateCard(card("4111 1111 1111 1111", "01/20", "123")));
        assertThrows(BusinessException.class, () -> service.validateCard(card("4111 1111 1111 1111", futureExpiry(), "12")));
        assertThrows(BusinessException.class, () -> service.validateCard(card("378282246310005", futureExpiry(), "123")));
    }

    @Test
    void simulatedGatewayDeclinesTestCard() {
        assertFalse(service.gatewayCharge(PaymentMethod.CARD, "4000000000000002", BigDecimal.TEN).approved());
        assertTrue(service.gatewayCharge(PaymentMethod.CARD, "4111111111111111", BigDecimal.TEN).approved());
        assertFalse(service.gatewayCharge(PaymentMethod.UPI, "fail@upi", BigDecimal.TEN).approved());
    }
}
