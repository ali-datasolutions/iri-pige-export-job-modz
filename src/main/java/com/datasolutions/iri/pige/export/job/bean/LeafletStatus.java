package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

/**
 * Created by romain on 09/04/2020
 */
@Getter
public enum LeafletStatus {
    IN_PROGRESS("IN PROGRESS"),
    FINISH("FINISH");

    private final String value;

    LeafletStatus(String value) {
        this.value = value;
    }

    public boolean isFinish() {
        return this.equals(FINISH);
    }

}
