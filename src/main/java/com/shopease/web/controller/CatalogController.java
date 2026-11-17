package com.shopease.web.controller;

import com.shopease.dao.ProductDao;
import com.shopease.model.Category;
import com.shopease.model.Page;
import com.shopease.model.Product;
import com.shopease.service.CatalogService;
import com.shopease.service.StoreSettings;
import com.shopease.web.WebUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/** Front-end of the Catalogue Service: browse, search, filter, product details. */
@Controller
public class CatalogController {

    private final CatalogService catalogService;
    private final StoreSettings settings;

    public CatalogController(CatalogService catalogService, StoreSettings settings) {
        this.catalogService = catalogService;
        this.settings = settings;
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String min,
                       @RequestParam(required = false) String max,
                       @RequestParam(required = false, defaultValue = "relevance") String sort,
                       @RequestParam(required = false) String inStock,
                       @RequestParam(required = false) String page,
                       Model model) {
        Category selected = null;
        if (category != null && !category.isBlank()) {
            selected = catalogService.listCategories().stream()
                    .filter(c -> c.getSlug().equals(category)).findFirst().orElse(null);
        }
        return render(q, selected, min, max, sort, inStock, page, model);
    }

    @GetMapping("/category/{slug}")
    public String byCategory(@PathVariable String slug,
                             @RequestParam(required = false) String min,
                             @RequestParam(required = false) String max,
                             @RequestParam(required = false, defaultValue = "relevance") String sort,
                             @RequestParam(required = false) String inStock,
                             @RequestParam(required = false) String page,
                             Model model) {
        Category selected = catalogService.getCategoryBySlug(slug);
        return render(null, selected, min, max, sort, inStock, page, model);
    }

    private String render(String q, Category selected, String min, String max, String sort, String inStock, String page, Model model) {
        BigDecimal minPrice = WebUtils.parseDecimal(min);
        BigDecimal maxPrice = WebUtils.parseDecimal(max);
        boolean inStockOnly = "on".equals(inStock) || "true".equals(inStock);
        ProductDao.Criteria criteria = new ProductDao.Criteria(q, selected == null ? null : selected.getId(),
                minPrice, maxPrice, inStockOnly, false, sort);
        Page<Product> result = catalogService.search(criteria, WebUtils.parseInt(page, 1), settings.getPageSize());

        model.addAttribute("result", result);
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("min", minPrice);
        model.addAttribute("max", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("inStock", inStockOnly);
        return "catalog/list";
    }

    @GetMapping("/products/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Product p = catalogService.getActiveProduct(id);
        model.addAttribute("product", p);
        model.addAttribute("related", catalogService.related(p, 4));
        return "catalog/detail";
    }
}
