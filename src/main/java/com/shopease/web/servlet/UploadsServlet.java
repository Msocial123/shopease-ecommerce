package com.shopease.web.servlet;

import com.shopease.service.ImageStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Serves images uploaded by the administrator: {@code GET /uploads/product-xxxx.png}.
 * The files live outside the web application (see {@link ImageStorage}), so they survive redeploys
 * and - in the container - a re-created container (named volume).
 */
@WebServlet(name = "uploadsServlet", urlPatterns = "/uploads/*")
public class UploadsServlet extends HttpServlet {

    private ImageStorage storage;

    @Override
    public void init() throws ServletException {
        storage = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext()).getBean(ImageStorage.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String info = req.getPathInfo();
        Optional<Path> file = info == null ? Optional.empty() : storage.resolve(info.substring(1));
        if (file.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path p = file.get();
        String name = p.getFileName().toString();
        String type = ImageStorage.CONTENT_TYPES.get(name.substring(name.lastIndexOf('.') + 1).toLowerCase());
        if (type == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType(type);
        resp.setContentLengthLong(Files.size(p));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "public, max-age=604800");
        Files.copy(p, resp.getOutputStream());
    }
}
