package com.tacs.tp1c2026;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global MVC configuration
 * Adds the /api prefix to all @RestController endpoints automatically, so individual controllers don't need to repeat it in @RequestMapping
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api",
            c -> c.isAnnotationPresent(RestController.class));
    }
}