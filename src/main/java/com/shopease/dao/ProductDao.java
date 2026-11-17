package com.shopease.dao;

import com.shopease.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ProductDao {

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name, c.slug AS category_slug " +
            "FROM products p JOIN categories c ON c.id = p.category_id ";

    /** Whitelisted ORDER BY clauses - never concatenate user input into SQL. */
    private static final Map<String, String> SORTS = Map.of(
            "newest", "p.created_at DESC, p.id DESC",
            "price_asc", "p.price ASC, p.id ASC",
            "price_desc", "p.price DESC, p.id ASC",
            "rating", "p.rating DESC, p.id ASC",
            "name", "p.name ASC",
            "relevance", "p.featured DESC, p.rating DESC, p.id ASC"
    );

    static final RowMapper<Product> MAPPER = (rs, i) -> {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setCategorySlug(rs.getString("category_slug"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setMrp(rs.getBigDecimal("mrp"));
        p.setStock(rs.getInt("stock"));
        p.setImageUrl(rs.getString("image_url"));
        p.setRating(rs.getBigDecimal("rating"));
        p.setFeatured(rs.getBoolean("featured"));
        p.setActive(rs.getBoolean("active"));
        p.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return p;
    };

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public ProductDao(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    /** Search criteria for catalogue listing. Null fields are ignored. */
    public record Criteria(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                           boolean inStockOnly, boolean includeInactive, String sort) {
    }

    public List<Product> search(Criteria c, int offset, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildWhere(c, params);
        String order = SORTS.getOrDefault(c.sort() == null ? "relevance" : c.sort(), SORTS.get("relevance"));
        params.addValue("limit", limit).addValue("offset", offset);
        return named.query(BASE_SELECT + where + " ORDER BY " + order + " LIMIT :limit OFFSET :offset", params, MAPPER);
    }

    public long count(Criteria c) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildWhere(c, params);
        Long n = named.queryForObject("SELECT COUNT(*) FROM products p JOIN categories c ON c.id = p.category_id " + where, params, Long.class);
        return n == null ? 0 : n;
    }

    private String buildWhere(Criteria c, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder("WHERE 1=1");
        if (!c.includeInactive()) {
            sb.append(" AND p.active = TRUE");
        }
        if (c.query() != null && !c.query().isBlank()) {
            sb.append(" AND (p.name LIKE :q OR p.brand LIKE :q OR p.description LIKE :q OR p.sku LIKE :q OR c.name LIKE :q)");
            params.addValue("q", "%" + escapeLike(c.query().trim()) + "%");
        }
        if (c.categoryId() != null) {
            sb.append(" AND p.category_id = :cat");
            params.addValue("cat", c.categoryId());
        }
        if (c.minPrice() != null) {
            sb.append(" AND p.price >= :min");
            params.addValue("min", c.minPrice());
        }
        if (c.maxPrice() != null) {
            sb.append(" AND p.price <= :max");
            params.addValue("max", c.maxPrice());
        }
        if (c.inStockOnly()) {
            sb.append(" AND p.stock > 0");
        }
        return sb.toString();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public Optional<Product> findById(long id) {
        return jdbc.query(BASE_SELECT + "WHERE p.id = ?", MAPPER, id).stream().findFirst();
    }

    public List<Product> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return named.query(BASE_SELECT + "WHERE p.id IN (:ids)", new MapSqlParameterSource("ids", ids), MAPPER);
    }

    public boolean skuExists(String sku, Long excludeId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE sku = ? AND id <> ?",
                Integer.class, sku, excludeId == null ? -1L : excludeId);
        return n != null && n > 0;
    }

    public List<Product> findFeatured(int limit) {
        return jdbc.query(BASE_SELECT + "WHERE p.active = TRUE AND p.featured = TRUE ORDER BY p.rating DESC, p.id LIMIT ?", MAPPER, limit);
    }

    public List<Product> findNewest(int limit) {
        return jdbc.query(BASE_SELECT + "WHERE p.active = TRUE ORDER BY p.created_at DESC, p.id DESC LIMIT ?", MAPPER, limit);
    }

    public List<Product> findRelated(long categoryId, long excludeId, int limit) {
        return jdbc.query(BASE_SELECT + "WHERE p.active = TRUE AND p.category_id = ? AND p.id <> ? ORDER BY p.rating DESC LIMIT ?",
                MAPPER, categoryId, excludeId, limit);
    }

    public List<Product> findLowStock(int threshold, int limit) {
        return jdbc.query(BASE_SELECT + "WHERE p.active = TRUE AND p.stock <= ? ORDER BY p.stock ASC LIMIT ?", MAPPER, threshold, limit);
    }

    public long insert(Product p) {
        KeyHolder kh = new GeneratedKeyHolder();
        MapSqlParameterSource ps = toParams(p);
        named.update("INSERT INTO products (category_id, sku, name, brand, description, price, mrp, stock, image_url, rating, featured, active) " +
                "VALUES (:categoryId, :sku, :name, :brand, :description, :price, :mrp, :stock, :imageUrl, :rating, :featured, :active)",
                ps, kh, new String[]{"id"});
        long id = Objects.requireNonNull(kh.getKey()).longValue();
        p.setId(id);
        return id;
    }

    public void update(Product p) {
        named.update("UPDATE products SET category_id = :categoryId, sku = :sku, name = :name, brand = :brand, " +
                "description = :description, price = :price, mrp = :mrp, stock = :stock, image_url = :imageUrl, " +
                "rating = :rating, featured = :featured, active = :active WHERE id = :id", toParams(p));
    }

    private MapSqlParameterSource toParams(Product p) {
        return new MapSqlParameterSource()
                .addValue("id", p.getId())
                .addValue("categoryId", p.getCategoryId())
                .addValue("sku", p.getSku())
                .addValue("name", p.getName())
                .addValue("brand", p.getBrand())
                .addValue("description", p.getDescription())
                .addValue("price", p.getPrice())
                .addValue("mrp", p.getMrp())
                .addValue("stock", p.getStock())
                .addValue("imageUrl", p.getImageUrl())
                .addValue("rating", p.getRating() == null ? BigDecimal.ZERO : p.getRating())
                .addValue("featured", p.isFeatured())
                .addValue("active", p.isActive());
    }

    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE products SET active = ? WHERE id = ?", active, id);
    }

    /**
     * Atomically reserves stock. Returns false when not enough units are left,
     * so two concurrent checkouts can never oversell the last item.
     */
    public boolean decrementStock(long productId, int qty) {
        return jdbc.update("UPDATE products SET stock = stock - ? WHERE id = ? AND active = TRUE AND stock >= ?",
                qty, productId, qty) == 1;
    }

    public void incrementStock(long productId, int qty) {
        jdbc.update("UPDATE products SET stock = stock + ? WHERE id = ?", qty, productId);
    }

    public long countActive() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE active = TRUE", Long.class);
        return n == null ? 0 : n;
    }
}
