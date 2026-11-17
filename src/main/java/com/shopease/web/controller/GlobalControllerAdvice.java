package com.shopease.web.controller;

import com.shopease.exception.BusinessException;
import com.shopease.exception.NotFoundException;
import com.shopease.model.Category;
import com.shopease.service.CartService;
import com.shopease.service.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Shared model attributes for every page + HTML error handling. */
@ControllerAdvice(basePackages = "com.shopease.web.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    private final CatalogService catalogService;
    private final CartService cartService;

    public GlobalControllerAdvice(CatalogService catalogService, CartService cartService) {
        this.catalogService = catalogService;
        this.cartService = cartService;
    }

    @ModelAttribute("navCategories")
    public List<Category> navCategories(HttpServletRequest req) {
        return req.getRequestURI().contains("/api/") ? List.of() : catalogService.listCategories();
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session, HttpServletRequest req) {
        return req.getRequestURI().contains("/api/") ? 0 : cartService.countItems(session);
    }

    @ExceptionHandler({NotFoundException.class, MethodArgumentTypeMismatchException.class, NoResourceFoundException.class})
    public ModelAndView notFound(Exception e, HttpServletResponse res) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        ModelAndView mv = new ModelAndView("error/404");
        mv.addObject("message", e instanceof NotFoundException ? e.getMessage() : "The page you requested does not exist.");
        return mv;
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException e, HttpServletResponse res) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("title", "We couldn't complete that");
        mv.addObject("message", e.getMessage());
        return mv;
    }

    @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
    public ModelAndView databaseDown(Exception e, HttpServletRequest req, HttpServletResponse res) {
        log.error("Database unavailable on {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("title", "Store temporarily unavailable");
        mv.addObject("message", "We can't reach the store database right now. Please try again in a few minutes. "
                + "(Administrators: check /health and the Tomcat log.)");
        return mv;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView methodNotAllowed(HttpServletResponse res) {
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("title", "Not allowed");
        mv.addObject("message", "That action is not allowed here.");
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e, HttpServletRequest req, HttpServletResponse res) {
        log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), e);
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("message", "Something went wrong on our side. Please try again in a moment.");
        return mv;
    }
}
