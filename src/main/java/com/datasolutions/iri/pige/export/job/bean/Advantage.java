package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by romain on 29/11/2019
 */
@Getter
public class Advantage {

    private final Long code;
    private final String description;

    public Advantage(Long code, String description) {
        this.code = checkNotNull(code);
        this.description = checkNotNull(description);
    }

}
