package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by romain on 23/12/2019
 */
@Getter
public class LeafletState {

    private final Long leafletId;
    private final UUID processUuid;
    private final Long processLastUpdate;

    public LeafletState(Long leafletId, UUID processUuid, Long processLastUpdate) {
        this.leafletId = checkNotNull(leafletId);
        this.processUuid = checkNotNull(processUuid);
        this.processLastUpdate = checkNotNull(processLastUpdate);
    }

}
