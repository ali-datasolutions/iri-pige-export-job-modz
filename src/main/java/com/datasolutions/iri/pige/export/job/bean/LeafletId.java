package com.datasolutions.iri.pige.export.job.bean;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by romain on 09/04/2020
 */
@Getter
@EqualsAndHashCode(of = "id")
public class LeafletId {

    private final Long id;
    private final LeafletStatus status;

    public LeafletId(Long id, LeafletStatus status) {
        this.id = checkNotNull(id);
        this.status = checkNotNull(status);
    }

}
