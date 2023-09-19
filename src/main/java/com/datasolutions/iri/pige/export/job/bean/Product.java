package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public class Product {

    private final Integer id;
    private final String label;
    private final String quantity;
    private final String gratuity;

    private final Integer sinOid;
    private final String brandName;

    public Product(Integer id, String label, String quantity, String gratuity, Integer sinOid, String brandName) {
        this.id = checkNotNull(id);
        this.label = checkNotNull(label);
        this.quantity = checkNotNull(quantity);
        this.gratuity = checkNotNull(gratuity);
        this.sinOid = checkNotNull(sinOid);
        this.brandName = checkNotNull(brandName);
    }

}
