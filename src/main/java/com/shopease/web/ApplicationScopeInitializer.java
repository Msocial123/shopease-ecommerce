package com.shopease.web;

import com.shopease.service.StoreSettings;
import jakarta.servlet.ServletContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

/**
 * Publishes store settings into application scope so every JSP (including container
 * error pages that never pass through Spring MVC) can use ${applicationScope.currency} etc.
 */
@Component
public class ApplicationScopeInitializer implements ServletContextAware {

    private final StoreSettings settings;

    public ApplicationScopeInitializer(StoreSettings settings) {
        this.settings = settings;
    }

    @Override
    public void setServletContext(ServletContext ctx) {
        ctx.setAttribute("storeName", settings.getStoreName());
        ctx.setAttribute("currency", settings.getCurrencySymbol());
        ctx.setAttribute("taxPercent", settings.getTaxPercent());
        ctx.setAttribute("freeShippingThreshold", settings.getFreeShippingThreshold());
        ctx.setAttribute("shippingFee", settings.getFlatShippingFee());
    }
}
