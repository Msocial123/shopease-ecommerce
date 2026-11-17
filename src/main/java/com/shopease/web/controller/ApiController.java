package com.shopease.web.controller;

import com.shopease.dao.ProductDao;
import com.shopease.model.Cart;
import com.shopease.model.Category;
import com.shopease.model.Order;
import com.shopease.model.Page;
import com.shopease.model.Product;
import com.shopease.service.CartService;
import com.shopease.service.CatalogService;
import com.shopease.service.OrderService;
import com.shopease.web.WebUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON REST API over the Catalogue, Cart and Orders services.
 * Used by the front-end JavaScript (AJAX add-to-cart) and usable by mobile / third-party clients.
 * State-changing calls require the {@code X-CSRF-Token} header.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final CatalogService catalogService;
    private final CartService cartService;
    private final OrderService orderService;

    public ApiController(CatalogService catalogService, CartService cartService, OrderService orderService) {
        this.catalogService = catalogService;
        this.cartService = cartService;
        this.orderService = orderService;
    }

    public record CartItemRequest(Long productId, Integer quantity) {
    }

    // ------------------------------------------------------------------ catalogue

    @GetMapping("/categories")
    public List<Category> categories() {
        return catalogService.listCategories();
    }

    @GetMapping("/products")
    public Page<Product> products(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false, defaultValue = "relevance") String sort,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "12") int size) {
        return catalogService.search(new ProductDao.Criteria(q, categoryId, null, null, false, false, sort), page, size);
    }

    @GetMapping("/products/{id:\\d+}")
    public Product product(@PathVariable long id) {
        return catalogService.getActiveProduct(id);
    }

    // ------------------------------------------------------------------ cart

    @GetMapping("/cart")
    public Cart cart(HttpSession session) {
        return cartService.getCart(session);
    }

    @PostMapping("/cart/items")
    public Map<String, Object> addToCart(@RequestBody CartItemRequest body, HttpSession session) {
        if (body == null || body.productId() == null) throw new com.shopease.exception.BusinessException("productId is required.");
        int qty = body.quantity() == null ? 1 : body.quantity();
        int lineQty = cartService.addItem(session, body.productId(), qty);
        return result("Added to cart", session, lineQty);
    }

    @PutMapping("/cart/items/{productId:\\d+}")
    public Map<String, Object> updateCartItem(@PathVariable long productId, @RequestBody CartItemRequest body, HttpSession session) {
        int qty = body == null || body.quantity() == null ? 0 : body.quantity();
        cartService.updateItem(session, productId, qty);
        return result("Cart updated", session, qty);
    }

    @DeleteMapping("/cart/items/{productId:\\d+}")
    public Map<String, Object> removeCartItem(@PathVariable long productId, HttpSession session) {
        cartService.removeItem(session, productId);
        return result("Removed from cart", session, 0);
    }

    private Map<String, Object> result(String message, HttpSession session, int lineQty) {
        Cart cart = cartService.getCart(session);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        m.put("lineQuantity", lineQty);
        m.put("cartCount", cart.getItemCount());
        m.put("cartTotal", cart.getTotal());
        return m;
    }

    // ------------------------------------------------------------------ orders (login required - AuthFilter)

    @GetMapping("/orders")
    public List<Order> orders(HttpSession session) {
        return orderService.listForUser(WebUtils.currentUser(session).getId());
    }

    @GetMapping("/orders/{orderNumber}")
    public Order order(@PathVariable String orderNumber, HttpSession session) {
        return orderService.getOrder(orderNumber, WebUtils.currentUser(session));
    }
}
