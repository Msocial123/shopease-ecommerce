package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.service.CartService;
import com.shopease.web.WebUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Front-end of the Cart Service. JavaScript-enabled pages use /api/cart instead for add-to-cart. */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String view(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCart(session));
        return "cart/cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam long productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false) String returnTo,
                      HttpSession session, RedirectAttributes ra) {
        try {
            cartService.addItem(session, productId, quantity);
            ra.addFlashAttribute("success", "Item added to your cart.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + WebUtils.safeRedirect(returnTo, "/cart");
    }

    @PostMapping("/update")
    public String update(@RequestParam long productId, @RequestParam int quantity, HttpSession session, RedirectAttributes ra) {
        try {
            cartService.updateItem(session, productId, quantity);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam long productId, HttpSession session, RedirectAttributes ra) {
        cartService.removeItem(session, productId);
        ra.addFlashAttribute("success", "Item removed from your cart.");
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes ra) {
        cartService.clear(session);
        ra.addFlashAttribute("success", "Your cart is now empty.");
        return "redirect:/cart";
    }
}
