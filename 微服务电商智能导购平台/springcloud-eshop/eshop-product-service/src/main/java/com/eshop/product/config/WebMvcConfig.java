package com.eshop.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 商品图片静态资源目录，对应 /images/** 请求。
     *
     * 默认为仓库内 frontend/public/images（相对启动目录，即从 springcloud-eshop/ 启动时向上一级）。
     * 若启动目录不同，请通过配置项 eshop.images.location 覆盖为绝对路径，例如：
     *   eshop.images.location=file:/data/eshop/images/
     */
    @Value("${eshop.images.location:file:../frontend/public/images/}")
    private String imagesLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(imagesLocation)
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}