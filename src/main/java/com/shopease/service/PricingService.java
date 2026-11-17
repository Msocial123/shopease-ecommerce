package com.shopease.service;

import com.shopease.model.Cart;
import com.shopease.model.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Pure pricing rules: subtotal, tax, shipping and total. No I/O, easy to unit-test. */
@Service
public class PricingService {

    private final BigDecimal taxRate;
    private final BigDecimal freeShippingThreshold;
    private final BigDecimal flatShippingFee;

    @Autowired
    public PricingService(StoreSettings settings) {
        this(settings.getTaxRate(), settings.getFreeShippingThreshold(), settings.getFlatShippingFee());
    }

    public PricingService(BigDecimal taxRate, BigDecimal freeShippingThreshold, BigDecimal flatShippingFee) {
        this.taxRate = taxRate;
        this.freeShippingThreshold = freeShippingThreshold;
        this.flatShippingFee = flatShippingFee;
    }

    public Cart price(List<CartItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal savings = BigDecimal.ZERO;
        for (CartItem it : items) {
            subtotal = subtotal.add(it.getLineTotal());
            BigDecimal mrp = it.getProduct().getMrp();
            if (mrp != null && mrp.compareTo(it.getProduct().getPrice()) > 0) {
                savings = savings.add(mrp.subtract(it.getProduct().getPrice()).multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }
        subtotal = money(subtotal);
        BigDecimal tax = money(subtotal.multiply(taxRate));
        BigDecimal shipping;
        BigDecimal remaining;
        if (items.isEmpty() || subtotal.compareTo(freeShippingThreshold) >= 0) {
            shipping = BigDecimal.ZERO;
            remaining = BigDecimal.ZERO;
        } else {
            shipping = flatShippingFee;
            remaining = freeShippingThreshold.subtract(subtotal);
        }
        shipping = money(shipping);
        BigDecimal total = money(subtotal.add(tax).add(shipping));
        return new Cart(items, subtotal, tax, shipping, total, money(savings), money(remaining));
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
