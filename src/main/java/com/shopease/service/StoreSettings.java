package com.shopease.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Store-wide settings from app.properties. */
@Component
public class StoreSettings {

    @Value("${app.store.name:ShopEase}")
    private String storeName;
    @Value("${app.currency.symbol:₹}")
    private String currencySymbol;
    @Value("${app.tax.rate:0.18}")
    private BigDecimal taxRate;
    @Value("${app.shipping.freeThreshold:999}")
    private BigDecimal freeShippingThreshold;
    @Value("${app.shipping.flatFee:49}")
    private BigDecimal flatShippingFee;
    @Value("${app.catalog.pageSize:12}")
    private int pageSize;

    public String getStoreName() { return storeName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getFreeShippingThreshold() { return freeShippingThreshold; }
    public BigDecimal getFlatShippingFee() { return flatShippingFee; }
    public int getPageSize() { return pageSize; }

    public int getTaxPercent() {
        return taxRate.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
