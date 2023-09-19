SELECT
    IFNULL(a.LEAFLET_CODE, -1) leafletCode,
    GROUP_CONCAT(DISTINCT a.OCC_PROD_CODE SEPARATOR ';') occProdCodes,
    a.EAN ean,
    IFNULL(b.ID , -1) promoCode,
    IFNULL(a.PROD_CODE, -1) prodCode,
    IFNULL(a.UB_CODE, -1) ubCode,
    IFNULL(a.SCOPE , -1) scope,
    IFNULL(a.STATUS_MATCH_EAN, 'UNKNOWN') statusMatchEan,
    IFNULL(a.MATCHING_TYPE, 'UNKNOWN') matchingType,
    a.IMPLICIT_UB implicit,
    IFNULL(a.OCC_PROD_DESCRIPTION, '-') occProdDescription,
    IFNULL(b.LIBELLE, '-') promoDescrDs,
    a.FG_COVER_PAGE fgCoverPage,
    IFNULL(a.NB_UNIT_IN_PACK, '0') nbUnitInPack,
    IFNULL(a.NB_UNIT_MIN_FOR_PROMO, '0') nbUnitMinForPromo,
    IFNULL(a.FG_LOYALTY_CARD, '0') fgLoyaltyCard,
    IFNULL(a.PRICE_PAID_INCL_REDUC, '0') pricePaidInclReduc,
    IFNULL(a.PRICE_BEFORE_REDUC,'0') priceBeforeReduc,
    IFNULL(a.PRICE_INSTANT_PROMO, '0') priceInstantPromo,
    IFNULL(a.PRICE_ALL_TYPES_PROMO, '0') priceAllTypesPromo,
    IFNULL(a.RATE_DISCOUNT_NIP, '0') rateDiscountNip,
    IFNULL(a.RATE_GRATUITY_ON_PACK, '0') rateGratuityOnPack,
    IFNULL(a.RATE_PRICE_CROSSED_OUT, '0') ratePriceCrossedOut,
    IFNULL(a.STARTING_DATE_PROMO, '00000000') startingDatePromo,
    IFNULL(a.ENDING_DATE_PROMO, '00000000') endingDatePromo,
    IFNULL(a.URL_PICTURE_UB, '-') urlPictureUb,
    IFNULL(a.URL_APPLI_UB, '-') urlAppliUb,
    IFNULL(a.DS_CREATE_DATE, '00000000') dsCreateDate,
    IFNULL(a.DS_UPDATE_DATE, '00000000') dsUpdateDate,
    a.SUPPORT_PRICE AS supportPrice,
    a.CROSSED_OUT_PRICE AS crossedOutPrice,
    a.NET_PRICE AS netPrice
FROM
    (SELECT
        s1.SUP_OID AS LEAFLET_CODE,
        o1.OCR_OID AS OCC_PROD_CODE,
        IF(ECO_STATUS IN (7, 8, 9), emao.ema_ean, null) AS EAN,
        0 AS PROMO_CODE,
        pr1.PRO_OID AS PROD_CODE,
        u1.UBE_OID AS UB_CODE,
        IF(sin1.sin_oid IN (:wineSegmentIds), sin1.sin_oid, IF(sin1.sin_oid IN (:bazaarSegmentIds), sin1.sin_oid, 'MATCHING')) AS SCOPE,
        CASE
            WHEN (sin1.sin_oid IN (:wineSegmentIds) OR sin1.sin_oid IN (:bazaarSegmentIds)) THEN 'NA'
            WHEN ECO_STATUS = -2 THEN 'OUT_OF_SCOPE'
            WHEN ECO_STATUS = -1 THEN 'UNKNOWN'
            WHEN ECO_STATUS = 0 THEN 'PENDING_UNTIL_CODIFICATION'
            WHEN ECO_STATUS IN (1, 2, 3, 4) THEN 'IN_PROGRESS_DS'
            WHEN ECO_STATUS IN (5, 6) THEN 'IN_PROGRESS_IRI'
            WHEN ECO_STATUS IN (7, 8, 9) THEN 'MATCH'
            ELSE 'IN_PROGRESS_DS'
        END AS STATUS_MATCH_EAN,
        CASE
            WHEN ECO_STATUS = 7 THEN 'MATCH_DS_MANUAL'
            WHEN ECO_STATUS = 8 THEN 'MATCH_IRI'
            WHEN ECO_STATUS = 9 THEN 'MATCH_IRI_MODS'
            ELSE 'NA'
        END AS MATCHING_TYPE,
        IF(UBE_VARIETE_EQUAL_OCR = -1, 1, 0) AS IMPLICIT_UB,
        CASE WHEN (UBE_VARIETE_EQUAL_OCR = 2) THEN CONCAT("GAMME ", pr1.PRO_DESIGNATION) ELSE pr1.PRO_DESIGNATION END AS OCC_PROD_DESCRIPTION,
        IF(p1.PIE_NUMERO = 1, true, false) AS FG_COVER_PAGE,
        uc1.uco_count AS NB_UNIT_IN_PACK,
        CASE WHEN (IFNULL(l1.lvi_taille_lot, 0) > 0) THEN l1.lvi_taille_lot Else 0  End AS NB_UNIT_MIN_FOR_PROMO,
        CASE WHEN (IFNULL(o1.car_oid, 0) > 0) THEN 1 ELSE 0 END AS FG_LOYALTY_CARD,
        CASE WHEN (IFNULL(o1.OCR_PRIX_SUPPORT, 0) <= 0) THEN 0 ELSE o1.OCR_PRIX_NET END AS PRICE_PAID_INCL_REDUC,
        CASE WHEN (IFNULL(o1.OCR_PRIX_SUPPORT, 0) <= 0) THEN 0 Else o1.OCR_PRIX_SUPPORT END AS PRICE_BEFORE_REDUC,
        (CASE WHEN (IFNULL(o1.OCR_PRIX_SUPPORT, 0) <= 0) THEN 0 Else o1.OCR_PRIX_SUPPORT END)
        - (CASE WHEN ((IFNULL(a1.AVA_LIBELLE, '') LIKE 'RI%') AND (IFNULL(o1.OCR_VALEUR_AVANTAGE, 0) > 0)) THEN
            (CASE WHEN ((ava_uni.UNI_LIBELLE = 'EURO') OR (a1.AVA_OID = 1226)) THEN ROUND(o1.OCR_VALEUR_AVANTAGE, 2)
                  WHEN (ava_uni.UNI_LIBELLE = '%') THEN
                        (CASE WHEN (IFNULL(o1.LVI_OID ,0) = 0) THEN ROUND(((o1.OCR_PRIX_SUPPORT * o1.OCR_VALEUR_AVANTAGE) / 100), 2)
                              WHEN (IFNULL(l1.LVI_REDUCTION_VAL_POURCENTAGE, 0) > 0) THEN ROUND((((((100 - l1.LVI_REDUCTION_VAL_POURCENTAGE) / 100) * o1.OCR_PRIX_SUPPORT) * o1.OCR_VALEUR_AVANTAGE) / 100), 2)
                              WHEN (IFNULL(o1.OCR_PRIX_LOTVIRTUEL, 0) > 0) THEN ROUND(((o1.OCR_PRIX_LOTVIRTUEL * o1.OCR_VALEUR_AVANTAGE) / 100), 2)
                              ELSE ROUND(((o1.OCR_PRIX_SUPPORT * o1.OCR_VALEUR_AVANTAGE) / 100),2)
                        END)
                  ELSE 0
            END)
            ELSE 0
        END) AS PRICE_INSTANT_PROMO,
        CASE WHEN (IFNULL(o1.OCR_PRIX_SUPPORT, 0) = 0) THEN 0
             WHEN (o1.OCR_PRIX_SUPPORT <= 0) THEN 0
             ELSE o1.OCR_PRIX_NET
        END AS PRICE_ALL_TYPES_PROMO,
        IFNULL(
            CASE WHEN ((IFNULL(o1.AVA_OID, 0) = 0) AND (IFNULL(o1.CAR_OID, 0) = 0) AND (IFNULL(o1.LVI_OID, 0) = 0)) THEN NULL
                 WHEN (o1.OCR_PRIX_SUPPORT > 0) THEN ROUND((GREATEST((o1.OCR_PRIX_SUPPORT - IFNULL(o1.OCR_PRIX_NET, 0)), 0) / o1.OCR_PRIX_SUPPORT), 5)
                 WHEN (IFNULL(o1.AVA_OID, 0) != 0) THEN o1.OCR_VALEUR_AVANTAGE
                 WHEN (IFNULL(o1.CAR_OID, 0) != 0) THEN o1.OCR_VALEUR_CARTE
                 WHEN (IFNULL(o1.LVI_OID, 0) != 0) THEN l1.LVI_REDUCTION_VAL_POURCENTAGE
                 ELSE NULL
            END
        , 0) AS RATE_DISCOUNT_NIP,
        IFNULL(pr1.PRO_REGLE_RESULTAT, 0) AS RATE_GRATUITY_ON_PACK,
        IFNULL(ROUND((CASE WHEN ((IFNULL(o1.OCR_PRIX_BARRE, 0)) > 0) THEN ((o1.OCR_PRIX_BARRE - o1.OCR_PRIX_SUPPORT) / o1.OCR_PRIX_BARRE)
                           ELSE 0
        END), 2), 0) AS RATE_PRICE_CROSSED_OUT,
        DATE_FORMAT(s1.SUP_DATE_DEBUT, '%Y%m%d') AS STARTING_DATE_PROMO,
        DATE_FORMAT(s1.SUP_DATE_FIN, '%Y%m%d') AS ENDING_DATE_PROMO,
        concat('https://pige-img-ws.retailexplorer.fr/pige-image/ws/ube/',  u1.UBE_OID , '.jpg',  CAST(CHAR(63) AS CHAR(2)) ,'height=0',  CHAR(38) ,'width=0') AS URL_PICTURE_UB,
        concat('http://recette-bov4.retailexplorer.fr/redirect.html', CAST(CHAR(63) AS CHAR(2)),'page=ube_unite_besoin/',  u1.UBE_OID , '/edit-occurences') AS URL_APPLI_UB,
        DATE_FORMAT(o1.OCR_CREATED_AT, '%Y%m%d') AS DS_CREATE_DATE,
        DATE_FORMAT(o1.OCR_UPDATED_AT, '%Y%m%d') AS DS_UPDATE_DATE,
        o1.OCR_PRIX_SUPPORT AS SUPPORT_PRICE,
        o1.OCR_PRIX_BARRE AS CROSSED_OUT_PRICE,
        o1.OCR_PRIX_NET AS NET_PRICE,
        o1.bri_oid,
        o1.lvi_oid,
        o1.AVA_OID, 
        o1.CAR_OID
    FROM
        sup_support s1
        INNER JOIN pie_piece p1 ON s1.sup_oid = p1.sup_oid
        INNER JOIN ube_unite_besoin u1 ON p1.pie_oid = u1.pie_oid
        INNER JOIN ocr_occurence o1 ON o1.ube_oid = u1.ube_oid
        LEFT JOIN LVI_LOT_VIRTUEL l1 ON o1.lvi_oid = l1.lvi_oid
        LEFT JOIN AVA_AVANTAGE a1 ON o1.ava_oid = a1.ava_oid
        LEFT JOIN CAR_CARTE c1 ON o1.car_oid = c1.car_oid
        LEFT JOIN uni_unite ava_uni ON ava_uni.UNI_OID = a1.UNI_OID
        LEFT JOIN uni_unite car_uni ON car_uni.UNI_OID = c1.UNI_OID
        INNER JOIN pro_produit pr1 ON o1.pro_oid = pr1.pro_oid
        INNER JOIN sin_sous_segment_invariant sin1 ON pr1.sin_oid = sin1.sin_oid
        INNER JOIN uco_unite_conditionnement uc1 ON pr1.uco_oid = uc1.uco_oid
        LEFT JOIN eco_ean_connection eco ON u1.UBE_OID = eco.UBE_OID
        LEFT JOIN noe_nn_ocr_ema noe ON o1.ocr_oid = noe.ocr_oid
        LEFT JOIN ema_ean_matched emao ON noe.ema_oid = emao.ema_oid
    WHERE
        s1.sup_oid IN (:leafletIds)
        AND (
            (sin1.SIN_MATCHING_EAN = 1 AND eco.UBE_OID IS NOT NULL)
            OR sin1.sin_oid IN (:wineSegmentIds)
            OR sin1.sin_oid IN (:bazaarSegmentIds)
        )
    ) a
    LEFT JOIN NOT_NN_OCR_TEC NOTC ON (NOTC.ocr_oid = a.OCC_PROD_CODE)
    LEFT JOIN (
        SELECT bri_oid + 1000000 ID ,bri_libelle LIBELLE
        FROM   BRI_BON_REDUC_IMMEDIATE
        WHERE  BRI_LIBELLE IS NOT NULL
        UNION ALL
        SELECT lvi_oid + 2000000 ID,concat('LV ', LVI_LIBELLE) LIBELLE
        FROM   LVI_LOT_VIRTUEL
        WHERE LVI_IRI_SCOPE = 1
        UNION ALL
        SELECT AVA_OID + 3000000 ID, AVA_LIBELLE LIBELLE
        FROM   AVA_AVANTAGE
        WHERE AVA_IRI_SCOPE = 1
        UNION ALL
        SELECT CAR_OID + 4000000 ID , CAR_LIBELLE LIBELLE
        FROM  CAR_CARTE
        WHERE CAR_IRI_SCOPE = 1
        UNION ALL
        SELECT TEC_OID + 7000000, concat('TECHNIQUE ', TEC_LIBELLE) LIBELLE
        FROM TEC_TECHNIQUE
        WHERE TEC_IRI_SCOPE = 1
    ) b ON ID IN (
        a.bri_oid + 1000000,
        a.lvi_oid + 2000000,
        a.AVA_OID + 3000000,
        a.CAR_OID + 4000000,
        NOTC.TEC_OID + 7000000
    )
GROUP BY leafletCode, prodCode, ean, promoCode
