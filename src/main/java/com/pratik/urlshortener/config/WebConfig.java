package com.pratik.urlshortener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * Handles CORS configuration.
 */
@Configuration
public class WebConfig
        implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(
            CorsRegistry registry
    ) {

        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5174",
                        "https://link-zen.vercel.app"
                )
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}