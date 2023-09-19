package com.datasolutions.iri.pige.export.job.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.sql2o.Sql2o;

import javax.sql.DataSource;

/**
 * Created by romain on 29/11/2019
 */
@Configuration
public class Sql2oConfiguration {

    @Bean
    @Primary
    public Sql2o pigeSql2o(DataSource dataSource) {
        return new Sql2o(dataSource);
    }

    @Bean
    @Qualifier("stateSql2o")
    @ConditionalOnProperty(name = "runner.incremental", havingValue = "true")
    public Sql2o stateSql2o(@Qualifier("stateDataSource") DataSource dataSource) {
        return new Sql2o(dataSource);
    }

}
