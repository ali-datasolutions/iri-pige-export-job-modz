package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

/**
 * Created by romain on 29/11/2019
 */
@Getter
public class LeafletStore {

    private final Long leafletCode;
    private final String storeCodeDs;
    private final String dsCreateDate;
    private final String dsUpdateDate;

    public LeafletStore(Long leafletCode, String storeCodeDs, String dsCreateDate, String dsUpdateDate) {
        this.leafletCode = leafletCode;
        this.storeCodeDs = storeCodeDs;
        this.dsCreateDate = dsCreateDate;
        this.dsUpdateDate = dsUpdateDate;
    }

}
