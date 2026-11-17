package com.shopease.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Bootstraps Spring MVC on a Servlet 6.1 container (Tomcat 11) without needing to
 * declare the DispatcherServlet in web.xml. Picked up automatically through
 * {@code SpringServletContainerInitializer} (ServletContainerInitializer SPI).
 *
 * <ul>
 *     <li>Root context  -> {@link RootConfig}  (DataSource, transactions, DAOs, services)</li>
 *     <li>Web context   -> {@link WebConfig}   (controllers, view resolver, static resources)</li>
 * </ul>
 */
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{WebConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected String getServletName() {
        return "shopease-dispatcher";
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // Allow multipart requests (5 MB max file / 10 MB request) for future upload features
        registration.setMultipartConfig(new MultipartConfigElement("", 5L * 1024 * 1024, 10L * 1024 * 1024, 0));
        registration.setLoadOnStartup(1);
    }
}
