package com.shopease.service;

import com.shopease.dao.CartDao;
import com.shopease.dao.OrderDao;
import com.shopease.dao.PaymentDao;
import com.shopease.dao.ProductDao;
import com.shopease.exception.BusinessException;
import com.shopease.exception.NotFoundException;
import com.shopease.model.Cart;
import com.shopease.model.CartItem;
import com.shopease.model.Order;
import com.shopease.model.OrderItem;
import com.shopease.model.OrderStatus;
import com.shopease.model.Page;
import com.shopease.model.Payment;
import com.shopease.model.PaymentMethod;
import com.shopease.model.PaymentStatus;
import com.shopease.model.SessionUser;
import com.shopease.model.ShippingAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Orders Service: checkout, order history, cancellation and fulfilment status. */
@Service
public class OrderService {

    private static final Pattern PHONE = Pattern.compile("^[0-9+\\- ]{7,15}$");
    private static final Pattern ZIP = Pattern.compile("^[A-Za-z0-9 \\-]{3,10}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final CartDao cartDao;
    private final PaymentDao paymentDao;
    private final PricingService pricing;

    public OrderService(OrderDao orderDao, ProductDao productDao, CartDao cartDao, PaymentDao paymentDao, PricingService pricing) {
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.cartDao = cartDao;
        this.paymentDao = paymentDao;
        this.pricing = pricing;
    }

    /**
     * Converts the user's cart into an order inside one transaction:
     * reserve stock -> create order + lines -> empty cart. Any failure rolls everything back.
     * The order starts as PENDING_PAYMENT; the Payment Service moves it on.
     */
    @Transactional
    public Order placeOrder(SessionUser user, ShippingAddress address, PaymentMethod method) {
        validateAddress(address);
        if (method == null) throw new BusinessException("Please choose a payment method.");

        // Re-read the cart and prices from the DB - never trust prices posted by the browser.
        Cart cart = pricing.price(cartDao.findByUser(user.getId()).stream().filter(i -> i.getProduct().isActive()).toList());
        if (cart.isEmpty()) throw new BusinessException("Your cart is empty.");

        for (CartItem item : cart.getItems()) {
            if (!productDao.decrementStock(item.getProduct().getId(), item.getQuantity())) {
                throw new BusinessException("Sorry, only " + item.getProduct().getStock() + " unit(s) of "
                        + item.getProduct().getName() + " are left. Please update your cart.");
            }
        }

        Order order = new Order();
        order.setOrderNumber(newOrderNumber());
        order.setUserId(user.getId());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(method);
        order.setSubtotal(cart.getSubtotal());
        order.setTax(cart.getTax());
        order.setShipping(cart.getShipping());
        order.setTotal(cart.getTotal());
        order.setShipName(address.getName().trim());
        order.setShipPhone(address.getPhone().trim());
        order.setShipLine1(address.getLine1().trim());
        order.setShipLine2(trimToNull(address.getLine2()));
        order.setShipCity(address.getCity().trim());
        order.setShipState(address.getState().trim());
        order.setShipZip(address.getZip().trim());
        order.setNotes(trimToNull(address.getNotes()));
        orderDao.insert(order);

        for (CartItem ci : cart.getItems()) {
            OrderItem it = new OrderItem();
            it.setOrderId(order.getId());
            it.setProductId(ci.getProduct().getId());
            it.setProductName(ci.getProduct().getName());
            it.setUnitPrice(ci.getProduct().getPrice());
            it.setQuantity(ci.getQuantity());
            it.setLineTotal(ci.getLineTotal());
            orderDao.insertItem(it);
        }
        cartDao.clear(user.getId());
        return order;
    }

    /** Loads an order with lines and payments; customers can only see their own orders. */
    public Order getOrder(String orderNumber, SessionUser viewer) {
        Order o = orderDao.findByNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!viewer.isAdmin() && !o.getUserId().equals(viewer.getId())) {
            throw new NotFoundException("Order not found");
        }
        o.setItems(orderDao.findItems(o.getId()));
        o.setPayments(paymentDao.findByOrder(o.getId()));
        return o;
    }

    public List<Order> listForUser(long userId) {
        return orderDao.findByUser(userId);
    }

    public Page<Order> listAll(OrderStatus status, String query, int page, int size) {
        int p = Math.max(1, page);
        long total = orderDao.countAll(status, query);
        return new Page<>(orderDao.findAll(status, query, (p - 1) * size, size), p, size, total);
    }

    public List<Order> recent(int limit) {
        return orderDao.findRecent(limit);
    }

    public Map<String, Long> countByStatus() {
        return orderDao.countByStatus();
    }

    /** Cancels an order, returns stock to inventory and refunds a captured payment. */
    @Transactional
    public void cancel(String orderNumber, SessionUser actor) {
        Order o = orderDao.findByNumberForUpdate(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!actor.isAdmin() && !o.getUserId().equals(actor.getId())) throw new NotFoundException("Order not found");
        if (!o.getStatus().isCancellable()) {
            throw new BusinessException("Order " + orderNumber + " can no longer be cancelled (status: " + o.getStatus().getLabel() + ").");
        }
        for (OrderItem it : orderDao.findItems(o.getId())) {
            productDao.incrementStock(it.getProductId(), it.getQuantity());
        }
        for (Payment p : paymentDao.findByOrder(o.getId())) {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                paymentDao.updateStatus(p.getId(), PaymentStatus.REFUNDED, "Refunded on cancellation");
            } else if (p.getStatus() == PaymentStatus.PENDING) {
                paymentDao.updateStatus(p.getId(), PaymentStatus.FAILED, "Order cancelled before delivery");
            }
        }
        orderDao.updateStatus(o.getId(), OrderStatus.CANCELLED);
    }

    /** Admin fulfilment flow: PAID/PLACED -> SHIPPED -> DELIVERED. Cancellation goes through {@link #cancel}. */
    @Transactional
    public void advanceStatus(String orderNumber, OrderStatus target, SessionUser admin) {
        if (target == OrderStatus.CANCELLED) {
            cancel(orderNumber, admin);
            return;
        }
        Order o = orderDao.findByNumberForUpdate(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        OrderStatus from = o.getStatus();
        boolean allowed = switch (target) {
            case SHIPPED -> from == OrderStatus.PAID || from == OrderStatus.PLACED;
            case DELIVERED -> from == OrderStatus.SHIPPED;
            default -> false;
        };
        if (!allowed) throw new BusinessException("Cannot move order from " + from.getLabel() + " to " + target.getLabel() + ".");
        orderDao.updateStatus(o.getId(), target);

        // Cash on delivery is collected when the parcel is delivered.
        if (target == OrderStatus.DELIVERED && o.getPaymentMethod() == PaymentMethod.COD) {
            for (Payment p : paymentDao.findByOrder(o.getId())) {
                if (p.getStatus() == PaymentStatus.PENDING) {
                    paymentDao.updateStatus(p.getId(), PaymentStatus.SUCCESS, "Cash collected on delivery");
                }
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private void validateAddress(ShippingAddress a) {
        if (a == null) throw new BusinessException("Shipping address is required.");
        require(a.getName(), "Full name");
        require(a.getPhone(), "Phone");
        require(a.getLine1(), "Address line 1");
        require(a.getCity(), "City");
        require(a.getState(), "State");
        require(a.getZip(), "PIN / ZIP code");
        if (!PHONE.matcher(a.getPhone().trim()).matches()) throw new BusinessException("Please enter a valid phone number.");
        if (!ZIP.matcher(a.getZip().trim()).matches()) throw new BusinessException("Please enter a valid PIN / ZIP code.");
        if (a.getNotes() != null && a.getNotes().length() > 500) throw new BusinessException("Delivery notes are limited to 500 characters.");
    }

    private static void require(String v, String label) {
        if (v == null || v.isBlank()) throw new BusinessException(label + " is required.");
        if (v.length() > 200) throw new BusinessException(label + " is too long.");
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String newOrderNumber() {
        return "SE" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
