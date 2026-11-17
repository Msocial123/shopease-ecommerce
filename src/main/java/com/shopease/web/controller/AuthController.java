package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.model.SessionUser;
import com.shopease.model.User;
import com.shopease.service.CartService;
import com.shopease.service.UserService;
import com.shopease.web.WebUtils;
import com.shopease.web.filter.CsrfFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final CartService cartService;

    public AuthController(UserService userService, CartService cartService) {
        this.userService = userService;
        this.cartService = cartService;
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String redirect, HttpSession session, Model model) {
        if (WebUtils.currentUser(session) != null) return "redirect:/";
        model.addAttribute("redirect", WebUtils.safeRedirect(redirect, ""));
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        @RequestParam(required = false) String redirect,
                        HttpServletRequest request, Model model, RedirectAttributes ra) {
        Optional<User> user = userService.authenticate(email, password);
        if (user.isEmpty()) {
            model.addAttribute("error", "Invalid email or password.");
            model.addAttribute("email", email);
            model.addAttribute("redirect", WebUtils.safeRedirect(redirect, ""));
            return "auth/login";
        }
        signIn(request, user.get());
        ra.addFlashAttribute("success", "Welcome back, " + SessionUser.of(user.get()).getFirstName() + "!");
        String fallback = user.get().isAdmin() ? "/admin" : "/";
        return "redirect:" + WebUtils.safeRedirect(redirect, fallback);
    }

    @GetMapping("/register")
    public String registerForm(HttpSession session) {
        if (WebUtils.currentUser(session) != null) return "redirect:/";
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam String password, @RequestParam String confirmPassword,
                           HttpServletRequest request, Model model, RedirectAttributes ra) {
        try {
            User u = userService.register(fullName, email, phone, password, confirmPassword);
            signIn(request, u);
            ra.addFlashAttribute("success", "Account created. Happy shopping, " + SessionUser.of(u).getFirstName() + "!");
            return "redirect:/";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes ra) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        // a fresh session carries the flash message
        request.getSession(true);
        ra.addFlashAttribute("success", "You have been logged out.");
        return "redirect:/";
    }

    private void signIn(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId(); // prevent session fixation
        session.setAttribute(SessionUser.SESSION_KEY, SessionUser.of(user));
        session.removeAttribute(CsrfFilter.SESSION_KEY); // rotate CSRF token on privilege change
        cartService.mergeGuestCart(session, user.getId());
    }
}
