package com.shopease.dao;

import com.shopease.config.DbDialect;
import com.shopease.model.CartItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Persistent cart for logged-in shoppers (guests use a session cart). */
@Repository
public class CartDao {

    private final JdbcTemplate jdbc;
    private final DbDialect dialect;

    public CartDao(JdbcTemplate jdbc, DbDialect dialect) {
        this.jdbc = jdbc;
        this.dialect = dialect;
    }

    public List<CartItem> findByUser(long userId) {
        return jdbc.query(
                "SELECT p.*, c.name AS category_name, c.slug AS category_slug, ci.quantity AS cart_qty " +
                "FROM cart_items ci JOIN products p ON p.id = ci.product_id JOIN categories c ON c.id = p.category_id " +
                "WHERE ci.user_id = ? ORDER BY ci.added_at, ci.id",
                (rs, i) -> new CartItem(ProductDao.MAPPER.mapRow(rs, i), rs.getInt("cart_qty")),
                userId);
    }

    public int getQuantity(long userId, long productId) {
        List<Integer> q = jdbc.queryForList("SELECT quantity FROM cart_items WHERE user_id = ? AND product_id = ?",
                Integer.class, userId, productId);
        return q.isEmpty() ? 0 : q.get(0);
    }

    /** Insert or overwrite the quantity of a line. */
    public void setQuantity(long userId, long productId, int qty) {
        String upsert = dialect.isPostgres()
                ? "ON CONFLICT (user_id, product_id) DO UPDATE SET quantity = EXCLUDED.quantity"
                : "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
        jdbc.update("INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?,?,?) " + upsert,
                userId, productId, qty);
    }

    public void remove(long userId, long productId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id = ? AND product_id = ?", userId, productId);
    }

    public void clear(long userId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id = ?", userId);
    }

    public int countItems(long userId) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM cart_items WHERE user_id = ?", Integer.class, userId);
        return n == null ? 0 : n;
    }
}
