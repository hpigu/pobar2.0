package com.pobar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file.upload-dir 是 uploads 根目錄（例如 ./uploads 或 /app/uploads）
        // StorageService 會在它底下建立 {folder}/{uuid.ext} 子路徑
        // URL /uploads/{folder}/{uuid.ext} 對應檔案 {upload-dir}/{folder}/{uuid.ext}
        // 結尾必須加 "/"，Spring 才會把它當資料夾解析
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
