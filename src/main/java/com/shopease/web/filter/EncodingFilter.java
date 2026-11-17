package com.shopease.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Forces UTF-8 on every request/response and adds baseline security headers. */
public class EncodingFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        chain.doFilter(req, res);
    }
}
