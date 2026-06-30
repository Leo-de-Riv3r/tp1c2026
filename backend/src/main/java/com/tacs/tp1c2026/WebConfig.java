package com.tacs.tp1c2026;

import com.tacs.tp1c2026.config.OwnerOrAdminInterceptor;
import com.tacs.tp1c2026.config.RoleInterceptor;
import com.tacs.tp1c2026.config.UserValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración global de MVC.
 * Agrega el prefijo /api a todos los endpoints @RestController automáticamente, de modo que los controladores individuales no necesitan repetirlo en @RequestMapping.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;
    private final OwnerOrAdminInterceptor ownerOrAdminInterceptor;
    private final UserValidationInterceptor userValidationInterceptor;

    public WebConfig(RoleInterceptor roleInterceptor, OwnerOrAdminInterceptor ownerOrAdminInterceptor, UserValidationInterceptor userValidationInterceptor) {
        this.roleInterceptor = roleInterceptor;
        this.ownerOrAdminInterceptor = ownerOrAdminInterceptor;
        this.userValidationInterceptor = userValidationInterceptor;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api",
            c -> c.isAnnotationPresent(RestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor);
        registry.addInterceptor(ownerOrAdminInterceptor);
        registry.addInterceptor(userValidationInterceptor);
    }
}