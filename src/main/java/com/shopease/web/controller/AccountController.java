package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.model.SessionUser;
import com.shopease.model.User;
import com.shopease.service.OrderService;
import com.shopease.service.UserService;
import com.shopease.web.WebUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;
    private final OrderService orderService;

    public AccountController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping
    public String profile(HttpSession session, Model model) {
        SessionUser su = WebUtils.currentUser(session);
        model.addAttribute("user", userService.get(su.getId()));
        model.addAttribute("orderCount", orderService.listForUser(su.getId()).size());
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName, @RequestParam(required = false) String phone,
                                HttpSession session, RedirectAttributes ra) {
        SessionUser su = WebUtils.currentUser(session);
        try {
            userService.updateProfile(su.getId(), fullName, phone);
            User fresh = userService.get(su.getId());
            session.setAttribute(SessionUser.SESSION_KEY, SessionUser.of(fresh));
            ra.addFlashAttribute("success", "Profile updated.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpSession session, RedirectAttributes ra) {
        try {
            userService.changePassword(WebUtils.currentUser(session).getId(), currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("success", "Password changed successfully.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account";
    }
}
