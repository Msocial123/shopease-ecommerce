package com.shopease.web.servlet;

import com.shopease.web.listener.SessionTrackingListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.time.Instant;

/**
 * Plain Jakarta Servlet (outside Spring MVC) exposing a health probe for load balancers / monitoring:
 * {@code GET /health} -> {"status":"UP","database":"UP",...}
 */
@WebServlet(name = "healthServlet", urlPatterns = "/health", loadOnStartup = 2)
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String db = "DOWN";
        String source = "unknown";
        try {
            WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
            source = ctx.getBean("dataSourceDescription", String.class);
            try (Connection c = ctx.getBean(DataSource.class).getConnection()) {
                if (c.isValid(2)) db = "UP";
            }
        } catch (Exception e) {
            log("Health check: database unavailable - " + e.getMessage());
        }
        boolean up = "UP".equals(db);
        resp.setStatus(up ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"status\":\"" + (up ? "UP" : "DEGRADED") + "\",\"database\":\"" + db
                + "\",\"dataSource\":\"" + source.replace("\"", "") + "\",\"activeSessions\":" + SessionTrackingListener.activeSessions()
                + ",\"server\":\"" + getServletContext().getServerInfo().replace("\"", "")
                + "\",\"servletApi\":\"" + getServletContext().getMajorVersion() + "." + getServletContext().getMinorVersion()
                + "\",\"time\":\"" + Instant.now() + "\"}");
    }
}
