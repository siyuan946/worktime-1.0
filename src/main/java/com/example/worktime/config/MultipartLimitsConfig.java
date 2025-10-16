// src/main/java/com/example/worktime/config/MultipartLimitsConfig.java
package com.example.worktime.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

/** 统一放开 Spring 的 multipart 限额（Java 8 / Boot 1.5 & 2.x 皆可） */
@Configuration
public class MultipartLimitsConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 字符串写法在 Boot 1.5 / 2.x 都支持
        factory.setMaxFileSize(DataSize.parse("200MB"));
        factory.setMaxRequestSize(DataSize.parse("200MB"));
        return factory.createMultipartConfig();
    }
}
