SELECT
    IFNULL(a.LEAFLET_CODE, '-1') leafletCode,
    IFNULL(a.OP_CODE, '-1') opCode,
    IFNULL(a.DESCRIPTION, '-') description,
    IFNULL(a.STARTING_DATE, '00000000') startingDate,
    IFNULL(a.ENDING_DATE, '00000000') endingDate,
    IFNULL(a.BANNER_CODE, '-1') bannerCode,
    IFNULL(a.BANNER_DESC, '-') bannerDescr,
    IFNULL(a.NB_STORE_BANNER, '0') nbStoreBanner,
    IFNULL(a.THEME, '-') theme,
    IFNULL(a.NB_PAGE_LEAFLET, '0') nbPageLeaflet,
    IFNULL(a.NB_STORE_OP, '0') nbStoreOp,
    IFNULL(a.NB_STORE_LEAFLET, '0') nbStoreLeaflet,
    IFNULL(a.NB_OCC_PROD, '0') nbOccProd,
    IFNULL(a.TYPE_OP, '-1') typeOp,
    IFNULL(a.TYPE_SUPPORT, '-1') typeSupport,
    IFNULL(a.URL_FIRST_PAGE, '0') urlFirstPage,
    IFNULL(a.URL_LAST_PAGE, '0') urlLastPage,
    IFNULL(a.DS_CREATE_DATE, '00000000') dsCreateDate,
    IFNULL(a.DS_UPDATE_DATE, '00000000') dsUpdateDate,
    IFNULL(a.STATUS, '0') status
FROM (
    SELECT
        s1.SUP_OID AS LEAFLET_CODE,
        o1.OPN_OID AS OP_CODE,
        s1.SUP_TITRE AS DESCRIPTION,
        DATE_FORMAT(s1.SUP_DATE_DEBUT, '%Y%m%d') AS STARTING_DATE,
        DATE_FORMAT(s1.SUP_DATE_FIN, '%Y%m%d') AS ENDING_DATE,
        s1.ENS_OID AS BANNER_CODE,
        e1.ENS_RAISON_SOCIALE AS BANNER_DESC,
        (
            SELECT COUNT(m1.mag_oid) FROM mag_magasin m1
            WHERE m1.ens_oid = e1.ENS_OID
        ) AS NB_STORE_BANNER,
        THE_LIBELLE AS THEME,
        SUP_NOMBRE_PAGES AS NB_PAGE_LEAFLET,
        (
            SELECT COUNT(DISTINCT n1.mag_oid) FROM opn_operation o2
            INNER JOIN sup_support s2 ON s2.opn_oid = o2.opn_oid
            INNER JOIN nsm_nn_sup_mag n1 ON n1.sup_oid = s2.sup_oid
            WHERE s2.opn_oid = o1.opn_oid AND s2.opn_oid = o2.opn_oid
        ) AS NB_STORE_OP,
        IFNULL(SUP_NOMBRE_MAGASINS, 0) AS NB_STORE_LEAFLET,
        COUNT(DISTINCT oc1.ocr_oid) AS NB_OCC_PROD,
        CASE o1.DER_OID WHEN 2 THEN 'R' ELSE 'N' END AS TYPE_OP,
        CASE
            WHEN tys1.tys_libelle = 'SITE WEB EXCLUSIF' THEN 'UB DIGITALE'
            WHEN tys1.tys_libelle = 'CATALOGUE PAPIER' THEN 'CATALOGUE'
            WHEN tys1.tys_libelle = 'CATALOGUE 100% DIGITAL' THEN 'CATALOGUE DIGITAL'
            ELSE tys1.tys_libelle END AS TYPE_SUPPORT,
        (
            SELECT CONCAT('https://pige-img-ws.retailexplorer.fr/pige-image/ws/piece/', MIN(i2.pie_oid), '.jpg',  CAST(CHAR(63) AS CHAR(2)), 'height=0', CHAR(38), 'width=0')
            FROM pie_piece i2, pie_piece i3
            WHERE i2.pie_numero < i3.pie_numero
            AND i2.sup_oid = s1.sup_oid
            AND i3.sup_oid = s1.sup_oid
        ) AS URL_FIRST_PAGE,
        (
            SELECT concat('https://pige-img-ws.retailexplorer.fr/pige-image/ws/piece/', MAX(i2.pie_oid), '.jpg', CAST(CHAR(63) AS CHAR(2)), 'height=0',  CHAR(38), 'width=0')
            FROM  pie_piece i2, pie_piece i3
            WHERE i2.pie_numero > i3.pie_numero
            AND i2.sup_oid = s1.sup_oid
            AND i3.sup_oid = s1.sup_oid
        ) AS URL_LAST_PAGE,
        DATE_FORMAT(SUP_CREATED_AT, '%Y%m%d') AS DS_CREATE_DATE,
        DATE_FORMAT(SUP_UPDATED_AT, '%Y%m%d') AS DS_UPDATE_DATE,
        s1.sup_statut AS STATUS
    FROM
        sup_support s1
        LEFT JOIN ens_enseigne e1 ON e1.ens_oid = s1.ens_oid
        LEFT JOIN opn_operation o1 ON o1.opn_oid = s1.opn_oid
        LEFT JOIN the_theme t1 ON t1.the_oid = o1.the_oid
        LEFT JOIN pie_piece i1 ON  s1.sup_oid = i1.sup_oid
        LEFT JOIN ube_unite_besoin u1 ON u1.pie_oid = i1.pie_oid
        LEFT JOIN ocr_occurence oc1 ON oc1.ube_oid = u1.ube_oid
        LEFT JOIN pro_produit pr1 ON oc1.pro_oid = pr1.pro_oid
        LEFT JOIN tys_type_support tys1 ON tys1.tys_oid = s1.tys_oid
    WHERE s1.sup_oid IN (:leafletIds)
    GROUP BY (s1.sup_oid)
) a
