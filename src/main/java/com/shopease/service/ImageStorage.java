package com.shopease.service;

import com.shopease.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores product images uploaded by the administrator on the local file system.
 *
 * <p>Folder: {@code app.upload.dir} (env {@code APP_UPLOAD_DIR}); default {@code ${catalina.base}/uploads}.
 * In the container this is {@code /usr/local/tomcat/uploads}, a named volume, so uploads survive
 * container re-creation. Files are served by {@code UploadsServlet} under {@code /uploads/*}.</p>
 *
 * <p>Only JPEG, PNG, GIF and WEBP are accepted, checked by the file's leading bytes (not just the
 * browser-supplied content type). SVG is refused on purpose: it can carry JavaScript.</p>
 */
@Component
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final Map<String, String> CONTENT_TYPES =
            Map.of("jpg", "image/jpeg", "png", "image/png", "gif", "image/gif", "webp", "image/webp");

    private final Path dir;

    public ImageStorage(@Value("${app.upload.dir:}") String configured) {
        String base = configured == null || configured.isBlank()
                ? System.getProperty("catalina.base", System.getProperty("java.io.tmpdir")) + "/uploads"
                : configured.trim();
        this.dir = Paths.get(base).toAbsolutePath().normalize();
        log.info("Uploaded images are stored in {}", dir);
    }

    public Path directory() {
        return dir;
    }

    /** Validates and saves an image; returns the URL to store in products.image_url (e.g. /uploads/product-ab12.png). */
    public String storeProductImage(byte[] data) {
        if (data == null || data.length == 0) throw new BusinessException("The uploaded image is empty.");
        if (data.length > MAX_BYTES) throw new BusinessException("Images may be at most 5 MB.");
        String ext = detectExtension(data);
        if (ext == null) throw new BusinessException("Only JPG, PNG, GIF or WEBP images can be uploaded.");
        String name = "product-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "." + ext;
        try {
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, ".upload-", ".tmp");
            Files.write(tmp, data);
            Files.move(tmp, dir.resolve(name), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new BusinessException("Could not save the image: " + e.getMessage());
        }
        log.info("Stored uploaded image {} ({} bytes)", name, data.length);
        return "/uploads/" + name;
    }

    /** Deletes a file previously returned by {@link #storeProductImage}; ignores anything else. */
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        resolve(url.substring("/uploads/".length())).ifPresent(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                log.warn("Could not delete {}: {}", p, e.getMessage());
            }
        });
    }

    /** Maps a file name from a /uploads/ URL to a file inside the upload folder, refusing path tricks. */
    public Optional<Path> resolve(String fileName) {
        if (fileName == null || !fileName.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")) return Optional.empty();
        Path p = dir.resolve(fileName).normalize();
        return p.startsWith(dir) && Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }

    private static String detectExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
