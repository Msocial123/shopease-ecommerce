package com.shopease.web.servlet;

import com.shopease.dao.ProductDao;
import com.shopease.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain Jakarta Servlet that renders an SVG placeholder image for products without an image URL:
 * {@code GET /images/product/{id}.svg}. Uses the Spring-managed {@link ProductDao}.
 */
@WebServlet(name = "productImageServlet", urlPatterns = "/images/product/*")
public class ProductImageServlet extends HttpServlet {

    private static final String[][] PALETTES = {
            {"#6366f1", "#8b5cf6"}, {"#0ea5e9", "#2563eb"}, {"#10b981", "#059669"}, {"#f59e0b", "#ea580c"},
            {"#ec4899", "#db2777"}, {"#14b8a6", "#0d9488"}, {"#ef4444", "#b91c1c"}, {"#8b5cf6", "#6d28d9"}
    };

    private ProductDao productDao;

    @Override
    public void init() throws ServletException {
        productDao = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext()).getBean(ProductDao.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String info = req.getPathInfo();
        if (info == null || !info.matches("^/\\d{1,18}(\\.svg)?$")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long id = Long.parseLong(info.replaceAll("\\D", ""));
        Optional<Product> product = productDao.findById(id);
        String name = product.map(Product::getName).orElse("ShopEase");
        String category = product.map(Product::getCategoryName).orElse("");

        String[] palette = PALETTES[(int) (id % PALETTES.length)];
        String initials = initials(name);
        List<String> lines = wrap(name, 22, 2);

        StringBuilder svg = new StringBuilder(1024);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 400' width='400' height='400'>")
                .append("<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>")
                .append("<stop offset='0' stop-color='").append(palette[0]).append("'/>")
                .append("<stop offset='1' stop-color='").append(palette[1]).append("'/>")
                .append("</linearGradient></defs>")
                .append("<rect width='400' height='400' fill='url(#g)'/>")
                .append("<circle cx='330' cy='70' r='120' fill='#fff' opacity='.08'/>")
                .append("<circle cx='60' cy='360' r='90' fill='#fff' opacity='.08'/>")
                .append("<circle cx='200' cy='165' r='78' fill='#fff' opacity='.18'/>")
                .append("<text x='200' y='190' text-anchor='middle' font-family='Segoe UI,Roboto,Arial,sans-serif' font-size='64' font-weight='700' fill='#fff'>")
                .append(xml(initials)).append("</text>");
        int y = 300;
        for (String line : lines) {
            svg.append("<text x='200' y='").append(y).append("' text-anchor='middle' font-family='Segoe UI,Roboto,Arial,sans-serif' font-size='22' font-weight='600' fill='#fff'>")
                    .append(xml(line)).append("</text>");
            y += 28;
        }
        if (!category.isEmpty()) {
            svg.append("<text x='200' y='").append(y + 8).append("' text-anchor='middle' font-family='Segoe UI,Roboto,Arial,sans-serif' font-size='14' fill='#fff' opacity='.8' letter-spacing='2'>")
                    .append(xml(category.toUpperCase())).append("</text>");
        }
        svg.append("</svg>");

        resp.setContentType("image/svg+xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "public, max-age=86400");
        resp.getWriter().write(svg.toString());
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.split("\\s+")) {
            if (!w.isEmpty() && Character.isLetterOrDigit(w.charAt(0))) sb.append(Character.toUpperCase(w.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.length() == 0 ? "SE" : sb.toString();
    }

    private static List<String> wrap(String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split("\\s+")) {
            if (cur.length() + w.length() + 1 > width && cur.length() > 0) {
                lines.add(cur.toString());
                cur.setLength(0);
                if (lines.size() == maxLines) break;
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (lines.size() < maxLines && cur.length() > 0) lines.add(cur.toString());
        if (lines.size() == maxLines && String.join(" ", lines).length() < text.length()) {
            String last = lines.get(maxLines - 1);
            lines.set(maxLines - 1, (last.length() > width - 1 ? last.substring(0, width - 1) : last) + "…");
        }
        return lines;
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;").replace("\"", "&quot;");
    }
}
