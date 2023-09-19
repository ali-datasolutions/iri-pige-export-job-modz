package com.datasolutions.iri.pige.export.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by romain on 29/11/2019
 */
@Slf4j
@SpringBootApplication
@ComponentScan("com.datasolutions.iri.pige.export.job")
public class Application {

    public static void main(String... args) {
        try {
            SpringApplication.run(Application.class);
            System.exit(0);
        } catch (Throwable throwable) {
            log.error("Application threw an exception!", throwable);
            System.exit(1);
        }
    }

}
