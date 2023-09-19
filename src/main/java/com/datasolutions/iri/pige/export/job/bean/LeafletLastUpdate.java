package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by romain on 23/12/2019
 */
@Getter
public class LeafletLastUpdate {

    private final Long leafletId;
    private final Long lastUpdate;

    public LeafletLastUpdate(Long leafletId, Long lastUpdate) {
        this.leafletId = checkNotNull(leafletId);
        this.lastUpdate = checkNotNull(lastUpdate);
    }

}
