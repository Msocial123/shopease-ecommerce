package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.service.OrderService;
import com.shopease.web.WebUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Front-end of the Orders Service for customers. */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("orders", orderService.listForUser(WebUtils.currentUser(session).getId()));
        return "orders/list";
    }

    @GetMapping("/{orderNumber}")
    public String detail(@PathVariable String orderNumber, HttpSession session, Model model) {
        model.addAttribute("order", orderService.getOrder(orderNumber, WebUtils.currentUser(session)));
        return "orders/detail";
    }

    @PostMapping("/{orderNumber}/cancel")
    public String cancel(@PathVariable String orderNumber, HttpSession session, RedirectAttributes ra) {
        try {
            orderService.cancel(orderNumber, WebUtils.currentUser(session));
            ra.addFlashAttribute("success", "Order " + orderNumber + " has been cancelled. Any payment made will be refunded.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + orderNumber;
    }
}
