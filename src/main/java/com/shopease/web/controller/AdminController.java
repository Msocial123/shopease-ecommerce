package com.shopease.web.controller;

import com.shopease.dao.OrderDao;
import com.shopease.dao.ProductDao;
import com.shopease.exception.BusinessException;
import com.shopease.model.Category;
import com.shopease.model.OrderStatus;
import com.shopease.model.Product;
import com.shopease.service.CatalogService;
import com.shopease.service.OrderService;
import com.shopease.service.UserService;
import com.shopease.web.WebUtils;
import com.shopease.web.listener.SessionTrackingListener;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Back-office: dashboard, catalogue management, order fulfilment, customers. Guarded by AuthFilter (ADMIN only). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int PAGE_SIZE = 15;

    private final CatalogService catalogService;
    private final OrderService orderService;
    private final UserService userService;
    private final OrderDao orderDao;
    private final ProductDao productDao;

    public AdminController(CatalogService catalogService, OrderService orderService, UserService userService,
                           OrderDao orderDao, ProductDao productDao) {
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.userService = userService;
        this.orderDao = orderDao;
        this.productDao = productDao;
    }

    /** Restrict which Product fields can be bound from the form (no mass-assignment of createdAt etc.). */
    @InitBinder("product")
    void productBinder(WebDataBinder binder) {
        binder.setAllowedFields("id", "categoryId", "sku", "name", "brand", "description", "price", "mrp",
                "stock", "imageUrl", "rating", "featured", "active");
    }

    @InitBinder("category")
    void categoryBinder(WebDataBinder binder) {
        binder.setAllowedFields("id", "name", "slug", "description");
    }

    // ------------------------------------------------------------------ dashboard

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("revenue", orderDao.revenue());
        model.addAttribute("statusCounts", orderService.countByStatus());
        model.addAttribute("customerCount", userService.countCustomers());
        model.addAttribute("productCount", productDao.countActive());
        model.addAttribute("orderCount", orderDao.countAll(null, null));
        model.addAttribute("activeSessions", SessionTrackingListener.activeSessions());
        model.addAttribute("recentOrders", orderService.recent(8));
        model.addAttribute("lowStock", catalogService.lowStock(10, 8));
        model.addAttribute("adminSection", "dashboard");
        return "admin/dashboard";
    }

    // ------------------------------------------------------------------ products

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q, @RequestParam(required = false) Long category,
                           @RequestParam(required = false) String page, Model model) {
        ProductDao.Criteria c = new ProductDao.Criteria(q, category, null, null, false, true, "newest");
        model.addAttribute("result", catalogService.search(c, WebUtils.parseInt(page, 1), PAGE_SIZE));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("adminSection", "products");
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        if (!model.containsAttribute("product")) {
            Product p = new Product();
            p.setStock(10);
            p.setRating(new BigDecimal("4.0"));
            model.addAttribute("product", p);
        }
        model.addAttribute("adminSection", "products");
        return "admin/product-form";
    }

    @GetMapping("/products/{id:\\d+}/edit")
    public String editProduct(@PathVariable long id, Model model) {
        if (!model.containsAttribute("product")) model.addAttribute("product", catalogService.getProduct(id));
        model.addAttribute("adminSection", "products");
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute("product") Product product, Model model, RedirectAttributes ra) {
        try {
            catalogService.saveProduct(product);
            ra.addFlashAttribute("success", "Product \"" + product.getName() + "\" saved.");
            return "redirect:/admin/products";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("adminSection", "products");
            return "admin/product-form";
        }
    }

    @PostMapping("/products/{id:\\d+}/toggle")
    public String toggleProduct(@PathVariable long id, RedirectAttributes ra) {
        Product p = catalogService.getProduct(id);
        catalogService.setProductActive(id, !p.isActive());
        ra.addFlashAttribute("success", "\"" + p.getName() + "\" is now " + (p.isActive() ? "hidden from" : "visible in") + " the store.");
        return "redirect:/admin/products";
    }

    // ------------------------------------------------------------------ categories

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", catalogService.listCategories());
        if (!model.containsAttribute("category")) model.addAttribute("category", new Category());
        model.addAttribute("adminSection", "categories");
        return "admin/categories";
    }

    @GetMapping("/categories/{id:\\d+}/edit")
    public String editCategory(@PathVariable long id, Model model) {
        model.addAttribute("category", catalogService.getCategory(id));
        return categories(model);
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("category") Category category, RedirectAttributes ra) {
        try {
            catalogService.saveCategory(category);
            ra.addFlashAttribute("success", "Category \"" + category.getName() + "\" saved.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("category", category);
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id:\\d+}/delete")
    public String deleteCategory(@PathVariable long id, RedirectAttributes ra) {
        try {
            catalogService.deleteCategory(id);
            ra.addFlashAttribute("success", "Category deleted.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ------------------------------------------------------------------ orders

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String status, @RequestParam(required = false) String q,
                         @RequestParam(required = false) String page, Model model) {
        OrderStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = OrderStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // unknown status -> show all
            }
        }
        model.addAttribute("result", orderService.listAll(st, q, WebUtils.parseInt(page, 1), PAGE_SIZE));
        model.addAttribute("status", st);
        model.addAttribute("q", q);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("adminSection", "orders");
        return "admin/orders";
    }

    @GetMapping("/orders/{orderNumber}")
    public String order(@PathVariable String orderNumber, HttpSession session, Model model) {
        model.addAttribute("order", orderService.getOrder(orderNumber, WebUtils.currentUser(session)));
        model.addAttribute("adminSection", "orders");
        return "admin/order";
    }

    @PostMapping("/orders/{orderNumber}/status")
    public String updateOrderStatus(@PathVariable String orderNumber, @RequestParam OrderStatus status,
                                    HttpSession session, RedirectAttributes ra) {
        try {
            orderService.advanceStatus(orderNumber, status, WebUtils.currentUser(session));
            ra.addFlashAttribute("success", "Order " + orderNumber + " marked as " + status.getLabel() + ".");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + orderNumber;
    }

    // ------------------------------------------------------------------ users

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.listAll());
        model.addAttribute("adminSection", "users");
        return "admin/users";
    }

    @PostMapping("/users/{id:\\d+}/toggle")
    public String toggleUser(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        try {
            var u = userService.get(id);
            userService.setActive(id, !u.isActive(), WebUtils.currentUser(session).getId());
            ra.addFlashAttribute("success", u.getEmail() + " has been " + (u.isActive() ? "deactivated" : "activated") + ".");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
