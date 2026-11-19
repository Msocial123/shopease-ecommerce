package com.shopease.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.JstlView;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DispatcherServlet context: controllers, JSP view resolution, static assets, JSON.
 */
@Configuration
@EnableWebMvc
@ComponentScan(
        basePackages = "com.shopease.web",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                classes = {Controller.class, RestController.class, ControllerAdvice.class}))
public class WebConfig implements WebMvcConfigurer {

    /** Uses the Servlet container's multipart support (limits set in AppInitializer) for MultipartFile parameters. */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp").viewClass(JstlView.class);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/assets/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("/assets/img/");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
    }
}
