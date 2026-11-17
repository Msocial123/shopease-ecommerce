package com.shopease.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Priced snapshot of a shopper's cart. Built by CartService. */
public class Cart {
    private final List<CartItem> items;
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal shipping;
    private final BigDecimal total;
    private final BigDecimal savings;
    private final BigDecimal freeShippingRemaining;

    public Cart(List<CartItem> items, BigDecimal subtotal, BigDecimal tax, BigDecimal shipping,
                BigDecimal total, BigDecimal savings, BigDecimal freeShippingRemaining) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.subtotal = subtotal;
        this.tax = tax;
        this.shipping = shipping;
        this.total = total;
        this.savings = savings;
        this.freeShippingRemaining = freeShippingRemaining;
    }

    public List<CartItem> getItems() { return items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getShipping() { return shipping; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getSavings() { return savings; }
    public BigDecimal getFreeShippingRemaining() { return freeShippingRemaining; }

    public boolean isEmpty() { return items.isEmpty(); }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isHasStockIssues() {
        return items.stream().anyMatch(CartItem::isExceedsStock);
    }
}
