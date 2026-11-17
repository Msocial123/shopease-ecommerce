package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.model.Order;
import com.shopease.model.OrderStatus;
import com.shopease.model.Payment;
import com.shopease.model.PaymentMethod;
import com.shopease.model.PaymentStatus;
import com.shopease.service.OrderService;
import com.shopease.service.PaymentRequest;
import com.shopease.service.PaymentService;
import com.shopease.web.WebUtils;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Front-end of the Payment Service. */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public PaymentController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @InitBinder("paymentRequest")
    void binder(WebDataBinder binder) {
        binder.setAllowedFields("method", "cardName", "cardNumber", "cardExpiry", "cardCvv", "upiId", "bank");
    }

    @GetMapping("/{orderNumber}")
    public String form(@PathVariable String orderNumber, HttpSession session, Model model) {
        Order order = orderService.getOrder(orderNumber, WebUtils.currentUser(session));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return "redirect:/orders/" + orderNumber;
        }
        model.addAttribute("order", order);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("banks", PaymentService.BANKS);
        model.addAttribute("codLimit", PaymentService.COD_LIMIT);
        return "payment/payment";
    }

    @PostMapping("/{orderNumber}")
    public String pay(@PathVariable String orderNumber, @ModelAttribute("paymentRequest") PaymentRequest req,
                      HttpSession session, RedirectAttributes ra) {
        try {
            Payment p = paymentService.pay(orderNumber, WebUtils.currentUser(session), req);
            if (p.getStatus() == PaymentStatus.FAILED) {
                ra.addFlashAttribute("error", "Payment failed: " + p.getMessage() + ". No money was charged - please try again or use another method.");
                return "redirect:/payment/" + orderNumber;
            }
            ra.addFlashAttribute("placed", true);
            ra.addFlashAttribute("success", p.getStatus() == PaymentStatus.PENDING
                    ? "Order placed! Please keep the cash ready at delivery."
                    : "Payment successful! Transaction " + p.getTransactionRef());
            return "redirect:/orders/" + orderNumber;
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/" + orderNumber;
        }
    }
}
