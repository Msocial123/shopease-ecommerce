package com.shopease.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.concurrent.atomic.AtomicInteger;

/** Counts active HTTP sessions (shown on the admin dashboard and /health). */
@WebListener
public class SessionTrackingListener implements HttpSessionListener, ServletContextListener {

    private static final AtomicInteger ACTIVE = new AtomicInteger();

    public static int activeSessions() {
        return ACTIVE.get();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().log("ShopEase started on " + sce.getServletContext().getServerInfo()
                + " (Servlet " + sce.getServletContext().getEffectiveMajorVersion() + "." + sce.getServletContext().getEffectiveMinorVersion() + ")");
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ACTIVE.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ACTIVE.updateAndGet(n -> Math.max(0, n - 1));
    }
}
