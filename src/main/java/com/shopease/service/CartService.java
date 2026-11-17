package com.shopease.service;

import com.shopease.dao.CartDao;
import com.shopease.dao.ProductDao;
import com.shopease.exception.BusinessException;
import com.shopease.model.Cart;
import com.shopease.model.CartItem;
import com.shopease.model.Product;
import com.shopease.model.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cart Service.
 * <ul>
 *     <li>Guests: cart lives in the HTTP session (productId -> quantity).</li>
 *     <li>Logged-in users: cart is persisted in the {@code cart_items} table.</li>
 *     <li>On login the guest cart is merged into the persistent cart.</li>
 * </ul>
 */
@Service
public class CartService {

    public static final String GUEST_CART_KEY = "guestCart";
    public static final int MAX_QTY_PER_LINE = 10;

    private final CartDao cartDao;
    private final ProductDao productDao;
    private final PricingService pricing;

    public CartService(CartDao cartDao, ProductDao productDao, PricingService pricing) {
        this.cartDao = cartDao;
        this.productDao = productDao;
        this.pricing = pricing;
    }

    public Cart getCart(HttpSession session) {
        SessionUser user = currentUser(session);
        List<CartItem> items;
        if (user != null) {
            items = cartDao.findByUser(user.getId());
        } else {
            Map<Long, Integer> guest = guestCart(session, false);
            items = new ArrayList<>();
            if (guest != null && !guest.isEmpty()) {
                for (Product p : productDao.findByIds(guest.keySet())) {
                    items.add(new CartItem(p, guest.get(p.getId())));
                }
                // keep insertion order of the session map
                List<Long> order = new ArrayList<>(guest.keySet());
                items.sort((a, b) -> Integer.compare(order.indexOf(a.getProduct().getId()), order.indexOf(b.getProduct().getId())));
            }
        }
        // silently drop products that were deactivated since they were added
        items.removeIf(i -> !i.getProduct().isActive());
        return pricing.price(items);
    }

    public int countItems(HttpSession session) {
        if (session == null) return 0;
        SessionUser user = currentUser(session);
        if (user != null) return cartDao.countItems(user.getId());
        Map<Long, Integer> guest = guestCart(session, false);
        return guest == null ? 0 : guest.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Adds quantity to a line. Returns the resulting line quantity. */
    @Transactional
    public int addItem(HttpSession session, long productId, int quantity) {
        if (quantity < 1) throw new BusinessException("Quantity must be at least 1.");
        Product p = loadPurchasable(productId);
        int current = currentQuantity(session, productId);
        int wanted = current + quantity;
        int max = Math.min(MAX_QTY_PER_LINE, p.getStock());
        if (current >= max) {
            throw new BusinessException(current >= MAX_QTY_PER_LINE
                    ? "You can buy at most " + MAX_QTY_PER_LINE + " units of an item per order."
                    : "Only " + p.getStock() + " unit(s) of " + p.getName() + " in stock.");
        }
        int newQty = Math.min(wanted, max);
        store(session, productId, newQty);
        return newQty;
    }

    /** Sets the quantity of a line; 0 removes it. */
    @Transactional
    public void updateItem(HttpSession session, long productId, int quantity) {
        if (quantity <= 0) {
            removeItem(session, productId);
            return;
        }
        Product p = loadPurchasable(productId);
        if (quantity > MAX_QTY_PER_LINE) throw new BusinessException("You can buy at most " + MAX_QTY_PER_LINE + " units of an item per order.");
        if (quantity > p.getStock()) throw new BusinessException("Only " + p.getStock() + " unit(s) of " + p.getName() + " in stock.");
        store(session, productId, quantity);
    }

    public void removeItem(HttpSession session, long productId) {
        SessionUser user = currentUser(session);
        if (user != null) {
            cartDao.remove(user.getId(), productId);
        } else {
            Map<Long, Integer> guest = guestCart(session, false);
            if (guest != null) {
                guest.remove(productId);
                session.setAttribute(GUEST_CART_KEY, guest); // re-set so replicated sessions see the change
            }
        }
    }

    public void clear(HttpSession session) {
        SessionUser user = currentUser(session);
        if (user != null) cartDao.clear(user.getId());
        else session.removeAttribute(GUEST_CART_KEY);
    }

    public void clearForUser(long userId) {
        cartDao.clear(userId);
    }

    /** Moves the guest (session) cart into the user's persistent cart after login. */
    @Transactional
    public void mergeGuestCart(HttpSession session, long userId) {
        Map<Long, Integer> guest = guestCart(session, false);
        if (guest == null || guest.isEmpty()) return;
        for (Product p : productDao.findByIds(guest.keySet())) {
            if (!p.isActive() || p.getStock() <= 0) continue;
            int merged = cartDao.getQuantity(userId, p.getId()) + guest.get(p.getId());
            int capped = Math.min(merged, Math.min(MAX_QTY_PER_LINE, p.getStock()));
            cartDao.setQuantity(userId, p.getId(), capped);
        }
        session.removeAttribute(GUEST_CART_KEY);
    }

    // ------------------------------------------------------------------ helpers

    private Product loadPurchasable(long productId) {
        Product p = productDao.findById(productId).orElseThrow(() -> new BusinessException("Product not found."));
        if (!p.isActive()) throw new BusinessException("This product is no longer available.");
        if (p.getStock() <= 0) throw new BusinessException(p.getName() + " is out of stock.");
        return p;
    }

    private int currentQuantity(HttpSession session, long productId) {
        SessionUser user = currentUser(session);
        if (user != null) return cartDao.getQuantity(user.getId(), productId);
        Map<Long, Integer> guest = guestCart(session, false);
        return guest == null ? 0 : guest.getOrDefault(productId, 0);
    }

    private void store(HttpSession session, long productId, int qty) {
        SessionUser user = currentUser(session);
        if (user != null) {
            cartDao.setQuantity(user.getId(), productId, qty);
        } else {
            Map<Long, Integer> guest = guestCart(session, true);
            guest.put(productId, qty);
            session.setAttribute(GUEST_CART_KEY, guest);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> guestCart(HttpSession session, boolean create) {
        Object o = session.getAttribute(GUEST_CART_KEY);
        if (o instanceof Map<?, ?> m) return (Map<Long, Integer>) m;
        if (!create) return null;
        Map<Long, Integer> m = new LinkedHashMap<>();
        session.setAttribute(GUEST_CART_KEY, m);
        return m;
    }

    private static SessionUser currentUser(HttpSession session) {
        return session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
    }
}
