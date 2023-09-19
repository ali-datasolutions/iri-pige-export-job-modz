package com.datasolutions.iri.pige.export.job;

import lombok.Getter;

/**
 * Created by romain on 04/12/2019
 */
@Getter
public enum  RunnerStatus {
    LEAFLET_STORES_DONE(10),
    STORES_DONE(11),
    LEAFLETS_DONE(12),
    LEAFLET_EANS_DONE(13),
    ADVANTAGES_DONE(14),
    ARCHIVE_DONE(15);

    private final int value;

    RunnerStatus(int value) {
        this.value = value;
    }

}
