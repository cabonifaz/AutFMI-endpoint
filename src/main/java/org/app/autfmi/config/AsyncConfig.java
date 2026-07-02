package org.app.autfmi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

  @Bean(name = "notificationExecutor")
  @Primary
  public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5); // Número mínimo de threads
    executor.setMaxPoolSize(10); // Número máximo de threads
    executor.setQueueCapacity(100); // Capacidad de la cola
    executor.setThreadNamePrefix("notification-");
    executor.initialize();
    return executor;
  }
}
