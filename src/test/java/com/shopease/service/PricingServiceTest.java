package com.shopease.service;

import com.shopease.model.Cart;
import com.shopease.model.CartItem;
import com.shopease.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingServiceTest {

    private final PricingService pricing = new PricingService(new BigDecimal("0.18"), new BigDecimal("999"), new BigDecimal("49"));

    private static Product product(String price, String mrp) {
        Product p = new Product();
        p.setId(1L);
        p.setName("Test");
        p.setPrice(new BigDecimal(price));
        p.setMrp(new BigDecimal(mrp));
        p.setStock(10);
        return p;
    }

    @Test
    void chargesShippingBelowThreshold() {
        Cart cart = pricing.price(List.of(new CartItem(product("100.00", "150.00"), 2)));
        assertEquals(new BigDecimal("200.00"), cart.getSubtotal());
        assertEquals(new BigDecimal("36.00"), cart.getTax());
        assertEquals(new BigDecimal("49.00"), cart.getShipping());
        assertEquals(new BigDecimal("285.00"), cart.getTotal());
        assertEquals(new BigDecimal("100.00"), cart.getSavings());
        assertEquals(new BigDecimal("799.00"), cart.getFreeShippingRemaining());
        assertEquals(2, cart.getItemCount());
    }

    @Test
    void freeShippingAtThreshold() {
        Cart cart = pricing.price(List.of(new CartItem(product("999.00", "999.00"), 1)));
        assertEquals(new BigDecimal("0.00"), cart.getShipping());
        assertEquals(new BigDecimal("1178.82"), cart.getTotal());
    }

    @Test
    void emptyCartCostsNothing() {
        Cart cart = pricing.price(List.of());
        assertTrue(cart.isEmpty());
        assertEquals(new BigDecimal("0.00"), cart.getTotal());
    }
}
