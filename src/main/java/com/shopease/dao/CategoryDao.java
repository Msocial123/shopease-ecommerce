package com.shopease.dao;

import com.shopease.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryDao {

    private static final String SELECT_WITH_COUNT =
            "SELECT c.*, (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.active = TRUE) AS product_count " +
            "FROM categories c ";

    private static final RowMapper<Category> MAPPER = (rs, i) -> {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        c.setDescription(rs.getString("description"));
        c.setProductCount(rs.getInt("product_count"));
        return c;
    };

    private final JdbcTemplate jdbc;

    public CategoryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Category> findAll() {
        return jdbc.query(SELECT_WITH_COUNT + "ORDER BY c.name", MAPPER);
    }

    public Optional<Category> findById(long id) {
        return jdbc.query(SELECT_WITH_COUNT + "WHERE c.id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Category> findBySlug(String slug) {
        return jdbc.query(SELECT_WITH_COUNT + "WHERE c.slug = ?", MAPPER, slug).stream().findFirst();
    }

    public void insert(Category c) {
        jdbc.update("INSERT INTO categories (name, slug, description) VALUES (?,?,?)",
                c.getName(), c.getSlug(), c.getDescription());
    }

    public void update(Category c) {
        jdbc.update("UPDATE categories SET name = ?, slug = ?, description = ? WHERE id = ?",
                c.getName(), c.getSlug(), c.getDescription(), c.getId());
    }

    public boolean hasProducts(long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE category_id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM categories WHERE id = ?", id);
    }
}
