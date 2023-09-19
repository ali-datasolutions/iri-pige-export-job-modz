package com.datasolutions.iri.pige.export.job.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Created by romain on 23/12/2019
 */
@Configuration
public class DataSourceConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties("pige.datasource")
    public DataSourceProperties pigeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("pige.datasource.hikari")
    public HikariDataSource pigeDataSource() {
        return pigeDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("state.datasource")
    @ConditionalOnProperty(name = "runner.incremental", havingValue = "true")
    public DataSourceProperties stateDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @LiquibaseDataSource
    @ConfigurationProperties("state.datasource.hikari")
    @ConditionalOnProperty(name = "runner.incremental", havingValue = "true")
    public HikariDataSource stateDataSource() {
        return stateDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

}
