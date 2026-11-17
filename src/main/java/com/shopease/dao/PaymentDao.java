package com.shopease.dao;

import com.shopease.model.Payment;
import com.shopease.model.PaymentMethod;
import com.shopease.model.PaymentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PaymentDao {

    private static final RowMapper<Payment> MAPPER = (rs, i) -> {
        Payment p = new Payment();
        p.setId(rs.getLong("id"));
        p.setOrderId(rs.getLong("order_id"));
        p.setTransactionRef(rs.getString("transaction_ref"));
        p.setMethod(PaymentMethod.valueOf(rs.getString("method")));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        p.setCardLast4(rs.getString("card_last4"));
        p.setCardBrand(rs.getString("card_brand"));
        p.setUpiId(rs.getString("upi_id"));
        p.setMessage(rs.getString("message"));
        p.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return p;
    };

    private final JdbcTemplate jdbc;

    public PaymentDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Payment p) {
        jdbc.update("INSERT INTO payments (order_id, transaction_ref, method, amount, status, card_last4, card_brand, upi_id, message) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)",
                p.getOrderId(), p.getTransactionRef(), p.getMethod().name(), p.getAmount(), p.getStatus().name(),
                p.getCardLast4(), p.getCardBrand(), p.getUpiId(), p.getMessage());
    }

    public List<Payment> findByOrder(long orderId) {
        return jdbc.query("SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC, id DESC", MAPPER, orderId);
    }

    public void updateStatus(long paymentId, PaymentStatus status, String message) {
        jdbc.update("UPDATE payments SET status = ?, message = ? WHERE id = ?", status.name(), message, paymentId);
    }
}
