package com.shopease.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private String shipName;
    private String shipPhone;
    private String shipLine1;
    private String shipLine2;
    private String shipCity;
    private String shipState;
    private String shipZip;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int itemCount;
    private List<OrderItem> items = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getShipName() { return shipName; }
    public void setShipName(String shipName) { this.shipName = shipName; }
    public String getShipPhone() { return shipPhone; }
    public void setShipPhone(String shipPhone) { this.shipPhone = shipPhone; }
    public String getShipLine1() { return shipLine1; }
    public void setShipLine1(String shipLine1) { this.shipLine1 = shipLine1; }
    public String getShipLine2() { return shipLine2; }
    public void setShipLine2(String shipLine2) { this.shipLine2 = shipLine2; }
    public String getShipCity() { return shipCity; }
    public void setShipCity(String shipCity) { this.shipCity = shipCity; }
    public String getShipState() { return shipState; }
    public void setShipState(String shipState) { this.shipState = shipState; }
    public String getShipZip() { return shipZip; }
    public void setShipZip(String shipZip) { this.shipZip = shipZip; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    /** java.util.Date view of createdAt for JSTL fmt:formatDate. */
    public java.util.Date getCreatedDate() {
        return createdAt == null ? null : java.sql.Timestamp.valueOf(createdAt);
    }
}
