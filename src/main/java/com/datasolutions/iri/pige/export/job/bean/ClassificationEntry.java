package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by romain on 10/07/2020
 */
@Getter
public class ClassificationEntry {

    private final Integer sinOid;
    private final String classification;
    private final String shelf;
    private final String category;
    private final String segment;
    private final String subSegment;
    private final String nonVaryingSubSegment;

    public ClassificationEntry(Integer sinOid, String classification, String shelf, String category, String segment, String subSegment, String nonVaryingSubSegment) {
        this.sinOid = checkNotNull(sinOid);
        this.classification = checkNotNull(classification);
        this.shelf = checkNotNull(shelf);
        this.category = checkNotNull(category);
        this.segment = checkNotNull(segment);
        this.subSegment = checkNotNull(subSegment);
        this.nonVaryingSubSegment = checkNotNull(nonVaryingSubSegment);
    }

}
