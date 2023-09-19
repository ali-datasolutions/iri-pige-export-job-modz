package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public class EanLink {

    private final String ean;
    private final String link;

    public EanLink(String ean, String link) {
        this.ean = checkNotNull(ean);
        this.link = checkNotNull(link);
    }

}
