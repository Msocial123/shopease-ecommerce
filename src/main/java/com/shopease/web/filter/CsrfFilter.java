package com.shopease.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Synchronizer-token CSRF protection.
 * <ul>
 *     <li>A random token is stored in the session and exposed as request attribute {@code csrfToken}.</li>
 *     <li>Forms send it as the hidden field {@code _csrf}; AJAX calls send header {@code X-CSRF-Token}.</li>
 *     <li>Every POST/PUT/PATCH/DELETE without a matching token is rejected with 403.</li>
 * </ul>
 */
public class CsrfFilter extends HttpFilter {

    public static final String SESSION_KEY = "CSRF_TOKEN";
    public static final String REQUEST_ATTR = "csrfToken";
    public static final String PARAM = "_csrf";
    public static final String HEADER = "X-CSRF-Token";

    private static final Set<String> UNSAFE = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // Static files, uploaded images and the health probe need no session (a session per
        // health check would pile up thousands of idle sessions).
        if (path.startsWith("/assets/") || path.startsWith("/images/") || path.startsWith("/uploads/")
                || path.equals("/health")) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            token = newToken();
            session.setAttribute(SESSION_KEY, token);
        }
        req.setAttribute(REQUEST_ATTR, token);

        if (UNSAFE.contains(req.getMethod())) {
            String sent = req.getHeader(HEADER);
            if (sent == null) sent = req.getParameter(PARAM);
            if (sent == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), sent.getBytes(StandardCharsets.UTF_8))) {
                if (path.startsWith("/api/")) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"success\":false,\"message\":\"Invalid or missing CSRF token. Please refresh the page.\"}");
                } else {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Your session expired or the form was invalid. Please go back, refresh and try again.");
                }
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
