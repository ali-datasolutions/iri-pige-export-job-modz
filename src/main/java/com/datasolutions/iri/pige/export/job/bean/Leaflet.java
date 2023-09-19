package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

/**
 * Created by romain on 29/11/2019
 */
@Getter
public class Leaflet {

    private final Long leafletCode;
    private final Long opCode;
    private final String description;
    private final String startingDate;
    private final String endingDate;
    private final Long bannerCode;
    private final String bannerDescr;
    private final Long nbStoreBanner;
    private final String theme;
    private final Long nbPageLeaflet;
    private final Long nbStoreOp;
    private final Long nbStoreLeaflet;
    private final Long nbOccProd;
    private final String typeOp;
    private final String typeSupport;
    private final String urlFirstPage;
    private final String urlLastPage;
    private final String dsCreateDate;
    private final String dsUpdateDate;
    private final LeafletStatus status;

    public Leaflet(Long leafletCode, Long opCode, String description, String startingDate, String endingDate, Long bannerCode, String bannerDescr, Long nbStoreBanner, String theme, Long nbPageLeaflet, Long nbStoreOp, Long nbStoreLeaflet, Long nbOccProd, String typeOp, String typeSupport, String urlFirstPage, String urlLastPage, String dsCreateDate, String dsUpdateDate, LeafletStatus status) {
        this.leafletCode = leafletCode;
        this.opCode = opCode;
        this.description = description;
        this.startingDate = startingDate;
        this.endingDate = endingDate;
        this.bannerCode = bannerCode;
        this.bannerDescr = bannerDescr;
        this.nbStoreBanner = nbStoreBanner;
        this.theme = theme;
        this.nbPageLeaflet = nbPageLeaflet;
        this.nbStoreOp = nbStoreOp;
        this.nbStoreLeaflet = nbStoreLeaflet;
        this.nbOccProd = nbOccProd;
        this.typeOp = typeOp;
        this.typeSupport = typeSupport;
        this.urlFirstPage = urlFirstPage;
        this.urlLastPage = urlLastPage;
        this.dsCreateDate = dsCreateDate;
        this.dsUpdateDate = dsUpdateDate;
        this.status = status;
    }

    @Getter
    public enum MatchStatus {
        IN_PROGRESS_DS("IN PROGRESS DS"),
        IN_PROGRESS_IRI("IN PROGRESS IRI"),
        FINISH("FINISH");

        private final String value;

        MatchStatus(String value) {
            this.value = value;
        }

    }

}
