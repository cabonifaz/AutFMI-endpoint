package org.app.autfmi.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {
    @Bean
    @Profile("prod")
    @Primary
    @ConfigurationProperties("spring.datasource.prod") // Set to use dev config yml
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public HikariDataSource dataSource(DataSourceProperties devDataSourceProperties) {
        return devDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Profile("prod")
    @Primary
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
