package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.model.Cart;
import com.shopease.model.Order;
import com.shopease.model.PaymentMethod;
import com.shopease.model.SessionUser;
import com.shopease.model.ShippingAddress;
import com.shopease.model.User;
import com.shopease.service.CartService;
import com.shopease.service.OrderService;
import com.shopease.service.UserService;
import com.shopease.web.WebUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Checkout: shipping address + payment method -> creates the order and hands over to payment. */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    public CheckoutController(CartService cartService, OrderService orderService, UserService userService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    public String form(HttpSession session, Model model, RedirectAttributes ra) {
        Cart cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            ra.addFlashAttribute("error", "Your cart is empty.");
            return "redirect:/cart";
        }
        if (cart.isHasStockIssues()) {
            ra.addFlashAttribute("error", "Some items in your cart exceed the available stock. Please update the quantities.");
            return "redirect:/cart";
        }
        if (!model.containsAttribute("address")) {
            SessionUser su = WebUtils.currentUser(session);
            User u = userService.get(su.getId());
            ShippingAddress a = new ShippingAddress();
            a.setName(u.getFullName());
            a.setPhone(u.getPhone());
            // Pre-fill from the most recent order, if any
            orderService.listForUser(su.getId()).stream().findFirst().ifPresent(last -> {
                a.setName(last.getShipName());
                a.setPhone(last.getShipPhone());
                a.setLine1(last.getShipLine1());
                a.setLine2(last.getShipLine2());
                a.setCity(last.getShipCity());
                a.setState(last.getShipState());
                a.setZip(last.getShipZip());
            });
            model.addAttribute("address", a);
        }
        model.addAttribute("cart", cart);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "checkout/checkout";
    }

    @PostMapping
    public String placeOrder(@ModelAttribute("address") ShippingAddress address,
                             @RequestParam(name = "paymentMethod", required = false) PaymentMethod method,
                             HttpSession session, Model model) {
        try {
            Order order = orderService.placeOrder(WebUtils.currentUser(session), address, method);
            return "redirect:/payment/" + order.getOrderNumber();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cart", cartService.getCart(session));
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("selectedMethod", method);
            return "checkout/checkout";
        }
    }
}
