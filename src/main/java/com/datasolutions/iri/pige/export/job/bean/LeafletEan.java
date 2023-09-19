package com.datasolutions.iri.pige.export.job.bean;

import lombok.Getter;

/**
 * Created by romain on 29/11/2019
 */
@Getter
public class LeafletEan {

    private final Long leafletCode;
    private final String occProdCodes;
    private final String ean;
    private final String promoCode;
    private final Long prodCode;
    private final String scope;
    private final MatchStatus statusMatchEan;
    private final MatchType matchingType;
    private final Boolean implicit;
    private final String occProdDescription;
    private final String promoDescrDs;
    private final Boolean fgCoverPage;
    private final Long nbUnitInPack;
    private final Long nbUnitMinForPromo;
    private final Boolean fgLoyaltyCard;
    private final Double pricePaidInclReduc;
    private final Double priceBeforeReduc;
    private final Double priceInstantPromo;
    private final Double priceAllTypesPromo;
    private final Double rateDiscountNip;
    private final Double rateGratuityOnPack;
    private final Double ratePriceCrossedOut;
    private final String startingDatePromo;
    private final String endingDatePromo;
    private final String urlPictureUb;
    private final String urlAppliUb;
    private final String dsCreateDate;
    private final String dsUpdateDate;
    private final Double supportPrice;
    private final Double crossedOutPrice;
    private final Double netPrice;
    private final Long ubCode;

    public LeafletEan(Long leafletCode, String occProdCodes, String ean, String promoCode, Long prodCode, String scope, MatchStatus statusMatchEan, MatchType matchingType, Boolean implicit, String occProdDescription, String promoDescrDs, Boolean fgCoverPage, Long nbUnitInPack, Long nbUnitMinForPromo, Boolean fgLoyaltyCard, Double pricePaidInclReduc, Double priceBeforeReduc, Double priceInstantPromo, Double priceAllTypesPromo, Double rateDiscountNip, Double rateGratuityOnPack, Double ratePriceCrossedOut, String startingDatePromo, String endingDatePromo, String urlPictureUb, String urlAppliUb, String dsCreateDate, String dsUpdateDate, Double supportPrice, Double crossedOutPrice, Double netPrice, Long ubCode) {
        this.leafletCode = leafletCode;
        this.occProdCodes = occProdCodes;
        this.ean = ean;
        this.promoCode = promoCode;
        this.prodCode = prodCode;
        this.scope = scope;
        this.statusMatchEan = statusMatchEan;
        this.matchingType = matchingType;
        this.implicit = implicit;
        this.occProdDescription = occProdDescription;
        this.fgCoverPage = fgCoverPage;
        this.promoDescrDs = promoDescrDs;
        this.nbUnitInPack = nbUnitInPack;
        this.nbUnitMinForPromo = nbUnitMinForPromo;
        this.fgLoyaltyCard = fgLoyaltyCard;
        this.pricePaidInclReduc = pricePaidInclReduc;
        this.priceBeforeReduc = priceBeforeReduc;
        this.priceInstantPromo = priceInstantPromo;
        this.priceAllTypesPromo = priceAllTypesPromo;
        this.rateDiscountNip = rateDiscountNip;
        this.rateGratuityOnPack = rateGratuityOnPack;
        this.ratePriceCrossedOut = ratePriceCrossedOut;
        this.startingDatePromo = startingDatePromo;
        this.endingDatePromo = endingDatePromo;
        this.urlPictureUb = urlPictureUb;
        this.urlAppliUb = urlAppliUb;
        this.dsCreateDate = dsCreateDate;
        this.dsUpdateDate = dsUpdateDate;
        this.supportPrice = supportPrice;
        this.crossedOutPrice = crossedOutPrice;
        this.netPrice = netPrice;
        this.ubCode = ubCode;
    }

    @Getter
    public enum MatchStatus {
        OUT_OF_SCOPE("OUT OF SCOPE"),
        UNKNOWN("UNKNOWN"),
        PENDING_UNTIL_CODIFICATION("PENDING UNTIL CODIF"),
        IN_PROGRESS_DS("IN PROGRESS DS"),
        IN_PROGRESS_IRI("IN PROGRESS IRI"),
        MATCH("MATCH"),
        NA("NA");

        private final String value;

        MatchStatus(String value) {
            this.value = value;
        }

    }

    @Getter
    public enum MatchType {
        MATCH_DS_MANUAL("MATCH DS MANUAL"),
        MATCH_DS_SEGMENTING("MATCH DS SEGMENTING"),
        MATCH_DS_MULTI("MATCH DS MULTI"),
        MATCH_IRI("MATCH IRI"),
        MATCH_IRI_MODS("MATCH IRI MODS"),
        NA("NA");

        private final String value;

        MatchType(String value) {
            this.value = value;
        }

    }

}
