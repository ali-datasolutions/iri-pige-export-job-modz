package com.datasolutions.iri.pige.export.job;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Created by romain on 29/11/2019
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "runner")
public class RunnerProperties {

    private String startDate;
    private String endDate;
    private Boolean validatedOnly = false;
    private Set<Integer> supportTypeIds;
    private Set<Integer> wineSegmentIds;
    private Set<Integer> bazaarSegmentIds;

    @NotNull
    private Upload upload;

    @NotNull
    private Boolean incremental;

    @Data
    public static class Upload {

        private Boolean enabled = false;

        private Sftp sftp;

        private Gcs gcs;

    }

    @Data
    public static class Gcs {

        @NotNull
        private String bucket;

    }

    @Data
    public static class Sftp {

        @NotNull
        private String hostname;

        @NotNull
        private Integer port;

        @NotNull
        private String username;

        @NotNull
        private String password;

    }

}
