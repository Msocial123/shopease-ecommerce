package com.shopease.service;

import com.shopease.dao.CategoryDao;
import com.shopease.dao.ProductDao;
import com.shopease.exception.BusinessException;
import com.shopease.exception.NotFoundException;
import com.shopease.model.Category;
import com.shopease.model.Page;
import com.shopease.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Catalogue Service: categories, product search/browse and product administration. */
@Service
public class CatalogService {

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://|/)[^\\s\"'<>]+$");

    private final ProductDao productDao;
    private final CategoryDao categoryDao;

    public CatalogService(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }

    // ------------------------------------------------------------------ browse

    public List<Category> listCategories() {
        return categoryDao.findAll();
    }

    public Category getCategoryBySlug(String slug) {
        return categoryDao.findBySlug(slug).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    public Category getCategory(long id) {
        return categoryDao.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    public Page<Product> search(ProductDao.Criteria criteria, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        long total = productDao.count(criteria);
        List<Product> rows = productDao.search(criteria, (safePage - 1) * safeSize, safeSize);
        return new Page<>(rows, safePage, safeSize, total);
    }

    /** Public product page: inactive products are hidden. */
    public Product getActiveProduct(long id) {
        Product p = getProduct(id);
        if (!p.isActive()) throw new NotFoundException("Product not found");
        return p;
    }

    public Product getProduct(long id) {
        return productDao.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    public List<Product> featured(int limit) {
        return productDao.findFeatured(limit);
    }

    public List<Product> newest(int limit) {
        return productDao.findNewest(limit);
    }

    public List<Product> related(Product p, int limit) {
        return productDao.findRelated(p.getCategoryId(), p.getId(), limit);
    }

    public List<Product> lowStock(int threshold, int limit) {
        return productDao.findLowStock(threshold, limit);
    }

    // ------------------------------------------------------------------ admin: products

    @Transactional
    public Product saveProduct(Product p) {
        validate(p);
        if (p.getId() == null) {
            productDao.insert(p);
        } else {
            getProduct(p.getId());
            productDao.update(p);
        }
        return p;
    }

    private void validate(Product p) {
        if (isBlank(p.getName())) throw new BusinessException("Product name is required.");
        if (isBlank(p.getSku())) throw new BusinessException("SKU is required.");
        p.setSku(p.getSku().trim().toUpperCase(Locale.ROOT));
        p.setName(p.getName().trim());
        if (p.getCategoryId() == null) throw new BusinessException("Please choose a category.");
        getCategory(p.getCategoryId());
        if (p.getPrice() == null || p.getPrice().signum() <= 0) throw new BusinessException("Price must be greater than zero.");
        if (p.getMrp() == null || p.getMrp().signum() <= 0) p.setMrp(p.getPrice());
        if (p.getMrp().compareTo(p.getPrice()) < 0) throw new BusinessException("MRP cannot be lower than the selling price.");
        if (p.getStock() < 0) throw new BusinessException("Stock cannot be negative.");
        if (p.getRating() != null && (p.getRating().compareTo(BigDecimal.ZERO) < 0 || p.getRating().compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new BusinessException("Rating must be between 0 and 5.");
        }
        if (!isBlank(p.getImageUrl())) {
            p.setImageUrl(p.getImageUrl().trim());
            if (!URL_PATTERN.matcher(p.getImageUrl()).matches()) {
                throw new BusinessException("Image URL must start with http://, https:// or /");
            }
        } else {
            p.setImageUrl(null);
        }
        if (productDao.skuExists(p.getSku(), p.getId())) throw new BusinessException("SKU " + p.getSku() + " is already used by another product.");
    }

    public void setProductActive(long id, boolean active) {
        getProduct(id);
        productDao.setActive(id, active);
    }

    // ------------------------------------------------------------------ admin: categories

    public void saveCategory(Category c) {
        if (isBlank(c.getName())) throw new BusinessException("Category name is required.");
        c.setName(c.getName().trim());
        c.setSlug(slugify(isBlank(c.getSlug()) ? c.getName() : c.getSlug()));
        if (c.getSlug().isEmpty()) throw new BusinessException("Category slug is invalid.");
        categoryDao.findBySlug(c.getSlug()).ifPresent(existing -> {
            if (!existing.getId().equals(c.getId())) throw new BusinessException("Slug '" + c.getSlug() + "' is already in use.");
        });
        if (c.getId() == null) categoryDao.insert(c);
        else categoryDao.update(c);
    }

    public void deleteCategory(long id) {
        if (categoryDao.hasProducts(id)) throw new BusinessException("Cannot delete a category that still has products.");
        categoryDao.delete(id);
    }

    public static String slugify(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
