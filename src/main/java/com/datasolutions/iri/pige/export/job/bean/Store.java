package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

/**
 * Created by romain on 29/11/2019
 */
@Getter
public class Store {

    private final String storeCodeDs;
    private final String storeCodeIri;
    private final String openDate;
    private final String closureDate;
    private final String lsaCode;
    private final String bannerCode;
    private final String bannerName;
    private final String addr1;
    private final String addr2;
    private final String zipcode;
    private final String town;
    private final String inseeCode;
    private final String longitude;
    private final String latitude;
    private final String dsCreateDate;
    private final String dsUpdateDate;

    public Store(String storeCodeDs, String storeCodeIri,String openDate, String closureDate, String lsaCode, String bannerCode, String bannerName, String addr1, String addr2, String zipcode, String town, String inseeCode, String longitude, String latitude, String dsCreateDate, String dsUpdateDate) {
        this.storeCodeDs = storeCodeDs;
        this.storeCodeIri = storeCodeIri;
        this.openDate = openDate;
        this.closureDate = closureDate;
        this.lsaCode = lsaCode;
        this.bannerCode = bannerCode;
        this.bannerName = bannerName;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.zipcode = zipcode;
        this.town = town;
        this.inseeCode = inseeCode;
        this.longitude = longitude;
        this.latitude = latitude;
        this.dsCreateDate = dsCreateDate;
        this.dsUpdateDate = dsUpdateDate;
    }

}
