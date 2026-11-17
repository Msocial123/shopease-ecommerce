package com.shopease.web;

import com.shopease.model.SessionUser;
import jakarta.servlet.http.HttpSession;

public final class WebUtils {

    private WebUtils() {
    }

    public static SessionUser currentUser(HttpSession session) {
        return session == null ? null : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
    }

    /** Only allow local, context-relative redirect targets (prevents open-redirects). */
    public static String safeRedirect(String target, String fallback) {
        if (target == null || target.isBlank()) return fallback;
        if (!target.startsWith("/") || target.startsWith("//") || target.contains("\\") || target.contains("\r") || target.contains("\n")) {
            return fallback;
        }
        if (target.startsWith("/login") || target.startsWith("/register") || target.startsWith("/logout")) return fallback;
        return target;
    }

    public static int parseInt(String s, int dflt) {
        try {
            return s == null || s.isBlank() ? dflt : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public static java.math.BigDecimal parseDecimal(String s) {
        try {
            return s == null || s.isBlank() ? null : new java.math.BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
