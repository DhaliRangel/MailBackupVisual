package com.mbv.config;

import com.mbv.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.mbv")
public class AppConfig {

    @Bean
    public CacheService cacheService() {
        return new CacheService();
    }

    @Bean
    public IndexingService indexingService() {
        return new IndexingService();
    }

    @Bean
    public SearchService searchService() {
        return new SearchService();
    }

    @Bean
    public ImportService importService() {
        return new ImportService();
    }

    @Bean
    public ThreadBuilder threadBuilder() {
        return new ThreadBuilder();
    }

    @Bean
    public ExportService exportService() {
        return new ExportService();
    }

    @Bean
    public ThemeManager themeManager() {
        return new ThemeManager();
    }
}
