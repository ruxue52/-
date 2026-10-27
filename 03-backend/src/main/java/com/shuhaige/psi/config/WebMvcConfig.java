package com.shuhaige.psi.config;

import com.shuhaige.psi.security.AuthInterceptor;
import com.shuhaige.psi.security.PermInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：CORS + 注册鉴权/权限拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final PermInterceptor permInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/qrcode/**",
                        "/scan/**",
                        "/error",
                        // 读者端（会员端）：公开接口免登录
                        "/reader/auth/**",
                        "/reader/books",
                        "/reader/books/**",
                        "/reader/categories",
                        "/reader/categories/**",
                        "/reader/levels"
                );
        // 权限拦截器必须晚于鉴权拦截器（依赖 UserContext 中的登录用户）
        registry.addInterceptor(permInterceptor)
                .order(2)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/qrcode/**",
                        "/scan/**",
                        "/error",
                        "/reader/auth/**",
                        "/reader/books",
                        "/reader/books/**",
                        "/reader/categories",
                        "/reader/categories/**",
                        "/reader/levels"
                );
    }
}