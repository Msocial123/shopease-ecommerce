package com.shopease.dao;

import com.shopease.model.Order;
import com.shopease.model.OrderItem;
import com.shopease.model.OrderStatus;
import com.shopease.model.PaymentMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class OrderDao {

    private static final String BASE_SELECT =
            "SELECT o.*, u.full_name AS customer_name, u.email AS customer_email, " +
            "(SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi WHERE oi.order_id = o.id) AS item_count " +
            "FROM orders o JOIN users u ON u.id = o.user_id ";

    private static final RowMapper<Order> MAPPER = (rs, i) -> {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setOrderNumber(rs.getString("order_number"));
        o.setUserId(rs.getLong("user_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerEmail(rs.getString("customer_email"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setTax(rs.getBigDecimal("tax"));
        o.setShipping(rs.getBigDecimal("shipping"));
        o.setTotal(rs.getBigDecimal("total"));
        o.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        o.setShipName(rs.getString("ship_name"));
        o.setShipPhone(rs.getString("ship_phone"));
        o.setShipLine1(rs.getString("ship_line1"));
        o.setShipLine2(rs.getString("ship_line2"));
        o.setShipCity(rs.getString("ship_city"));
        o.setShipState(rs.getString("ship_state"));
        o.setShipZip(rs.getString("ship_zip"));
        o.setNotes(rs.getString("notes"));
        o.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        o.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        o.setItemCount(rs.getInt("item_count"));
        return o;
    };

    private static final RowMapper<OrderItem> ITEM_MAPPER = (rs, i) -> {
        OrderItem it = new OrderItem();
        it.setId(rs.getLong("id"));
        it.setOrderId(rs.getLong("order_id"));
        it.setProductId(rs.getLong("product_id"));
        it.setProductName(rs.getString("product_name"));
        it.setImageUrl(rs.getString("image_url"));
        it.setUnitPrice(rs.getBigDecimal("unit_price"));
        it.setQuantity(rs.getInt("quantity"));
        it.setLineTotal(rs.getBigDecimal("line_total"));
        return it;
    };

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public OrderDao(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    public long insert(Order o) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("orderNumber", o.getOrderNumber())
                .addValue("userId", o.getUserId())
                .addValue("status", o.getStatus().name())
                .addValue("subtotal", o.getSubtotal())
                .addValue("tax", o.getTax())
                .addValue("shipping", o.getShipping())
                .addValue("total", o.getTotal())
                .addValue("paymentMethod", o.getPaymentMethod().name())
                .addValue("shipName", o.getShipName())
                .addValue("shipPhone", o.getShipPhone())
                .addValue("shipLine1", o.getShipLine1())
                .addValue("shipLine2", o.getShipLine2())
                .addValue("shipCity", o.getShipCity())
                .addValue("shipState", o.getShipState())
                .addValue("shipZip", o.getShipZip())
                .addValue("notes", o.getNotes());
        KeyHolder kh = new GeneratedKeyHolder();
        named.update("INSERT INTO orders (order_number, user_id, status, subtotal, tax, shipping, total, payment_method, " +
                "ship_name, ship_phone, ship_line1, ship_line2, ship_city, ship_state, ship_zip, notes) VALUES " +
                "(:orderNumber, :userId, :status, :subtotal, :tax, :shipping, :total, :paymentMethod, " +
                ":shipName, :shipPhone, :shipLine1, :shipLine2, :shipCity, :shipState, :shipZip, :notes)", p, kh, new String[]{"id"});
        long id = Objects.requireNonNull(kh.getKey()).longValue();
        o.setId(id);
        return id;
    }

    public void insertItem(OrderItem it) {
        jdbc.update("INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, line_total) VALUES (?,?,?,?,?,?)",
                it.getOrderId(), it.getProductId(), it.getProductName(), it.getUnitPrice(), it.getQuantity(), it.getLineTotal());
    }

    public Optional<Order> findByNumber(String orderNumber) {
        return jdbc.query(BASE_SELECT + "WHERE o.order_number = ?", MAPPER, orderNumber).stream().findFirst();
    }

    /** Locks the order row for the rest of the transaction (prevents double payment / double cancel). */
    public Optional<Order> findByNumberForUpdate(String orderNumber) {
        return jdbc.query(BASE_SELECT + "WHERE o.order_number = ? FOR UPDATE", MAPPER, orderNumber).stream().findFirst();
    }

    public List<OrderItem> findItems(long orderId) {
        return jdbc.query("SELECT oi.*, p.image_url FROM order_items oi LEFT JOIN products p ON p.id = oi.product_id " +
                "WHERE oi.order_id = ? ORDER BY oi.id", ITEM_MAPPER, orderId);
    }

    public List<Order> findByUser(long userId) {
        return jdbc.query(BASE_SELECT + "WHERE o.user_id = ? ORDER BY o.created_at DESC, o.id DESC", MAPPER, userId);
    }

    public List<Order> findAll(OrderStatus status, String query, int offset, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = where(status, query, p);
        p.addValue("limit", limit).addValue("offset", offset);
        return named.query(BASE_SELECT + where + " ORDER BY o.created_at DESC, o.id DESC LIMIT :limit OFFSET :offset", p, MAPPER);
    }

    public long countAll(OrderStatus status, String query) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        Long n = named.queryForObject("SELECT COUNT(*) FROM orders o JOIN users u ON u.id = o.user_id " + where(status, query, p), p, Long.class);
        return n == null ? 0 : n;
    }

    private String where(OrderStatus status, String query, MapSqlParameterSource p) {
        StringBuilder sb = new StringBuilder("WHERE 1=1");
        if (status != null) {
            sb.append(" AND o.status = :status");
            p.addValue("status", status.name());
        }
        if (query != null && !query.isBlank()) {
            sb.append(" AND (o.order_number LIKE :q OR u.email LIKE :q OR u.full_name LIKE :q)");
            p.addValue("q", "%" + query.trim() + "%");
        }
        return sb.toString();
    }

    public void updateStatus(long orderId, OrderStatus status) {
        jdbc.update("UPDATE orders SET status = ? WHERE id = ?", status.name(), orderId);
    }

    public void updatePaymentMethod(long orderId, PaymentMethod method) {
        jdbc.update("UPDATE orders SET payment_method = ? WHERE id = ?", method.name(), orderId);
    }

    public BigDecimal revenue() {
        BigDecimal r = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total),0) FROM orders WHERE status IN ('PAID','SHIPPED','DELIVERED')", BigDecimal.class);
        return r == null ? BigDecimal.ZERO : r;
    }

    /** status -> count, in enum order. */
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) result.put(s.name(), 0L);
        jdbc.query("SELECT status, COUNT(*) AS n FROM orders GROUP BY status",
                rs -> {
                    result.put(rs.getString("status"), rs.getLong("n"));
                });
        return result;
    }

    public List<Order> findRecent(int limit) {
        return jdbc.query(BASE_SELECT + "ORDER BY o.created_at DESC, o.id DESC LIMIT ?", MAPPER, limit);
    }
}
