package com.shopease.web.filter;

import com.shopease.model.SessionUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Authentication / authorisation gate.
 * <ul>
 *     <li>{@code /checkout, /payment, /orders, /account, /api/orders} require a logged-in user.</li>
 *     <li>{@code /admin/**} additionally requires the ADMIN role.</li>
 * </ul>
 */
public class AuthFilter extends HttpFilter {

    private static final List<String> USER_AREAS = List.of("/checkout", "/payment", "/orders", "/account", "/api/orders");
    private static final String ADMIN_AREA = "/admin";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean adminArea = matches(path, ADMIN_AREA);
        boolean userArea = adminArea || USER_AREAS.stream().anyMatch(p -> matches(path, p));

        if (!userArea) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = req.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);

        if (user == null) {
            if (path.startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"success\":false,\"message\":\"Please log in.\"}");
                return;
            }
            String target = path + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
            if (!"GET".equals(req.getMethod())) target = "/";
            res.sendRedirect(req.getContextPath() + "/login?redirect=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
            return;
        }

        if (adminArea && !user.isAdmin()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access required.");
            return;
        }
        // Authenticated pages must not be cached by the browser (back button after logout).
        res.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, res);
    }

    private static boolean matches(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
