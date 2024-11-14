package org.app.autfmi.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {

    /*
     * Prod
     */
    @Bean
    @Profile("prod")
    @ConfigurationProperties("spring.datasource.prod")
    public DataSourceProperties prodDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource prodDataSource(DataSourceProperties blogDataSourceProperties) {
        return blogDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    JdbcTemplate prodJdbcTemplate(@Qualifier("prodDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /*
     * Preprod
     */
    @Bean
    @Profile("preprod")
    @ConfigurationProperties("spring.datasource.preprod")
    public DataSourceProperties preprodDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource preprodDataSource(@Qualifier("preprodDataSourceProperties") DataSourceProperties preprodDataSourceProperties) {
        return preprodDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    JdbcTemplate preprodJdbcTemplate(@Qualifier("preprodDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /*
     * DEV
     */
    @Bean
    @Profile("dev")
    @ConfigurationProperties("spring.datasource.dev")
    public DataSourceProperties devDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public HikariDataSource devDataSource(@Qualifier("devDataSourceProperties") DataSourceProperties devDataSourceProperties) {
        return devDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    JdbcTemplate devJdbcTemplate(@Qualifier("devDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
