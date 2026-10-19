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
                        // Azure BDT Preprod
                        "https://zealous-plant-02486730f.2.azurestaticapps.net",
                        "https://bancotalentobackendpreprod-awdecbbsgrh4d8bn.canadacentral-01.azurewebsites.net",
                        // Azure FMI Preprod
                        "https://salmon-rock-06416070f.2.azurestaticapps.net",
                        "https://autfmibackendpreprod-hadmdsa5hjaghub8.canadacentral-01.azurewebsites.net",
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