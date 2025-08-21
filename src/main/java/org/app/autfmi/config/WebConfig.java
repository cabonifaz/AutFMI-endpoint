package org.app.autfmi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:3001",
                        // Azure BDT Staging
                        "https://happy-forest-038bd820f.1.azurestaticapps.net",
                        "https://bancotalentobackendstaging-gee7h5b8exe6gkhb.canadacentral-01.azurewebsites.net",
                        // Azure FMI Staging
                        "https://thankful-glacier-088d1980f.2.azurestaticapps.net",
                        "https://autfmibackendstaging-gnfub6d8cdg5aqbd.canadacentral-01.azurewebsites.net",
                        // AWS BDT Staging
                        "https://bancotalentos.aplicacion2-team.com",
                        "https://api-bdt.aplicacion2-team.com",
                        // AWS FMI Staging
                        "https://autfmi.aplicacion2-team.com",
                        "https://api-fmi.aplicacion2-team.com"
                ).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }
}
